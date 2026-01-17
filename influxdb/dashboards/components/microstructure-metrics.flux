import "math"

excluded = [v.not1, v.not2, v.not3]
start    = duration(v: v.rangeD)

sign = (x) => if x > 0.0 then 1 else if x < 0.0 then -1 else 0

condAggregate = (tables=<-) =>
  if      v.rangeD == "-2d" then tables |> aggregateWindow(every: duration(v: string(v: 2 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-3d" then tables |> aggregateWindow(every: duration(v: string(v: 3 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-4d" then tables |> aggregateWindow(every: duration(v: string(v: 4 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-7d" then tables |> aggregateWindow(every: duration(v: string(v: 5 * 3) + "m"), fn: mean, createEmpty: false)  
  else tables

base = (measurement, fieldRegex) =>
  from(bucket: "stream-lines-market-data-realtime")
    |> range(start: start, stop: now())
    |> filter(fn: (r) => r._measurement == measurement)
    |> filter(fn: (r) => r._field =~ fieldRegex)
    |> filter(fn: (r) => r.ticker == v.ticker or v.ticker == "")
    |> filter(fn: (r) => length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))
    |> group(columns: ["ticker", "_field"])
    |> keep(columns: ["_time", "_field", "_value", "ticker"])

// !nb knows nothing that would require understanding semantics behind those arguments
makeTab = (measurement, fieldRegex) =>
  base(
    measurement: measurement,
    fieldRegex: fieldRegex
  )
  |> filter(fn: (r) => r._value != 0.0 or not exists r._value) // if exists, must be nonzero
  |> condAggregate()

wide = union(tables: [
  makeTab(
    measurement: "stock-quotation-aggregates-sliding-window",
    fieldRegex: /^(volume_weighted_avg_ask_price|volume_weighted_avg_bid_price|sum_ask_quantity|sum_bid_quantity|sum_ask_notional|sum_bid_notional)$/
  ),
  makeTab(
    measurement: "stock-trade-aggregates-sliding-window",
    fieldRegex: /^(volume_weighted_avg_price|sum_quantity|sum_notional)$/,
  )
])
  |> group(columns: ["ticker"])
  |> pivot(rowKey: ["_time", "ticker"], columnKey: ["_field"], valueColumn: "_value")
  |> rename(columns: {
      volume_weighted_avg_ask_price: "vwap_ask",
      volume_weighted_avg_bid_price: "vwap_bid",
      volume_weighted_avg_price: "vwap_price",
      sum_ask_notional: "ask_notional",
      sum_bid_notional: "bid_notional",
      sum_notional: "trade_notional",
      sum_ask_quantity: "ask_quantity",
      sum_bid_quantity: "bid_quantity",
      sum_quantity: "trade_quantity",
  })
  |> filter(fn: (r) => exists r.vwap_ask and exists r.ask_quantity and exists r.ask_notional)
  |> filter(fn: (r) => exists r.vwap_bid and exists r.bid_quantity and exists r.bid_notional)
  |> filter(fn: (r) => exists r.vwap_price and exists r.trade_quantity and exists r.trade_notional)
  |> sort(columns: ["_time"])

delta = wide
  |> group(columns: ["ticker"])
  |> keep(columns: ["_time","ticker","vwap_ask","vwap_bid","vwap_price"])
  |> difference(columns: ["vwap_ask", "vwap_bid", "vwap_price"], keepFirst: false, nonNegative: false)
  |> filter(fn: (r) => r.vwap_price != 0.0) // not sure what brings these in
  |> rename(columns: {
    vwap_ask: "delta_vwap_ask",
    vwap_bid: "delta_vwap_bid",
    vwap_price: "delta_vwap_price"
  })

withDelta = join(tables: {w: wide, d: delta}, on: ["_time", "ticker"])
    |> map(fn: (r) => ({
      r with
      prev_vwap_ask:   r.vwap_ask   - r.delta_vwap_ask,
      prev_vwap_bid:   r.vwap_bid   - r.delta_vwap_bid,
      prev_vwap_price: r.vwap_price - r.delta_vwap_price
    }))
  |> filter(fn: (r) => r.prev_vwap_ask != 0.0)
  |> filter(fn: (r) => r.prev_vwap_bid != 0.0)
  |> filter(fn: (r) => r.prev_vwap_price != 0.0)

metrics = withDelta
  |> map(fn: (r) => {
    mid = (r.vwap_bid + r.vwap_ask) / 2.0

    prevSpreadRate = (r.prev_vwap_ask - r.prev_vwap_bid)
                   / (r.prev_vwap_ask + r.prev_vwap_bid) * 100.0

    spreadRate = (r.vwap_ask - r.vwap_bid)
               / (r.vwap_ask + r.vwap_bid) * 100.0

    spreadRateWithFloor = if spreadRate < 0.01 then 0.01 else spreadRate

    askChangeRate = (r.prev_vwap_ask - r.vwap_ask)
                  / (r.prev_vwap_ask + r.vwap_ask) * 100.0

    bidChangeRate = (r.vwap_bid - r.prev_vwap_bid)
                  / (r.vwap_bid + r.prev_vwap_bid) * 100.0

    sumQuoteNotional = r.bid_notional + r.ask_notional

    return {
      _time: r._time,
      ticker: r.ticker,

      askChangeRate: askChangeRate,
      bidChangeRate: bidChangeRate,

      // intent
      //                |          +          |       0      |           -          |
      // bid - prev bid |   Buyers concede    |  Buyers hold |    Buyers retreat    |
      // prev ask - ask |   Sellers concede   | Sellers hold |   Sellers retreat    |
      //   concession   | Buyers concede mode |              | Sellers concede more |
      concession: bidChangeRate - askChangeRate,

      imbalance:
        if sumQuoteNotional > 0.0 then
          (r.bid_notional - r.ask_notional) / sumQuoteNotional
        else
          0.0,

      midChange: 0.5 * (bidChangeRate - askChangeRate),

      //          >         0          <
      //   Spread narrows   |     Spread widens
      //        Push        |     Retreat, Fear
      // Liquidity improves | Liquidity deteriorates
      spreadChange: prevSpreadRate - spreadRate,

      askNotional: r.ask_notional,
      bidNotional: r.bid_notional,
      tradeNotional: r.trade_notional,
      askQuantity: r.ask_quantity,
      bidQuantity: r.bid_quantity,
      tradeQuantity: r.trade_quantity,
  
      //            >           |     0    |           <            | 
      //   VWAP closer to ask   | balanced |  VWAP closer to bid    |
      // Seller-initiated trade |          | Buyer-initiated trade  |
      //   Aggressive seller    |          |    Aggressive buyer    |
      tradePosition:
        if spreadRate > 0.0 then
            (r.vwap_price - mid)
          / (r.vwap_price + mid) * 100.0
          / spreadRateWithFloor
        else
          0.0,
    }
  })

result = metrics
  |> map(fn: (r) => ({ r with
    agree: if sign(x: r.concession) == sign(x: r.tradePosition) then 1 else 0,

    // did concession become reality?
    //            >           |      0     |           <            | 
    // Q concede, T follow    |            | Q condede, T disagree  |
    // Q pressure moves price | indecision | Q pressure is absorbed |
    agreeScore: (1.0 + r.concession) * r.tradePosition,
  }))

yValue = (tables=<-, name, valueFn, cumulative=false) =>
  (if bool(v: v.cumul) and bool(v: cumulative) then
     tables |> cumulativeSum(columns: [name])
   else
     tables)
    |> map(fn: (r) => ({
      _time: r._time,
      _field: name,
      _value: float(v: valueFn(r: r)),
      ticker: r.ticker
    }))
    |> keep(columns: ["_time", "_field", "_value", "ticker"])

// rates
if      v.microSt == "agreeScore"    then result |> yValue(name: "agreeScore",    valueFn: (r) => r.agreeScore,    cumulative: true)
else if v.microSt == "askChangeRate" then result |> yValue(name: "askChangeRate", valueFn: (r) => r.askChangeRate, cumulative: true)
else if v.microSt == "bidChangeRate" then result |> yValue(name: "bidChangeRate", valueFn: (r) => r.bidChangeRate, cumulative: true)
else if v.microSt == "concession"    then result |> yValue(name: "concession",    valueFn: (r) => r.concession,    cumulative: true)
else if v.microSt == "imbalance"     then result |> yValue(name: "imbalance",     valueFn: (r) => r.imbalance,     cumulative: true)
else if v.microSt == "midChange"     then result |> yValue(name: "midChange",     valueFn: (r) => r.midChange,     cumulative: true)
else if v.microSt == "spreadChange"  then result |> yValue(name: "spreadChange",  valueFn: (r) => r.spreadChange,  cumulative: true)
else if v.microSt == "tradePosition" then result |> yValue(name: "tradePosition", valueFn: (r) => r.tradePosition, cumulative: true)

// dollars
else if v.microSt == "askNotional"   then result |> yValue(name: "askNotional",   valueFn: (r) => r.askNotional)
else if v.microSt == "askQuantity"   then result |> yValue(name: "askQuantity",   valueFn: (r) => r.askQuantity)
else if v.microSt == "bidNotional"   then result |> yValue(name: "bidNotional",   valueFn: (r) => r.bidNotional)
else if v.microSt == "bidQuantity"   then result |> yValue(name: "bidQuantity",   valueFn: (r) => r.bidQuantity)
else if v.microSt == "tradeNotional" then result |> yValue(name: "tradeNotional", valueFn: (r) => r.tradeNotional)
else if v.microSt == "tradeQuantity" then result |> yValue(name: "tradeQuantity", valueFn: (r) => r.tradeQuantity)

else                                      result |> yValue(name: "midChange",     valueFn: (r) => r.midChange,     cumulative: true)
