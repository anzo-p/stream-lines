import "math"

excluded = [v.not1, v.not2, v.not3]
start    = duration(v: v.rangeD)
step     = 3m

sign = (x) => if x > 0.0 then 1 else if x < 0.0 then -1 else 0

quotes = from(bucket: "stream-lines-market-data-realtime")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r._measurement == "stock-quotation-aggregates-sliding-window")
  |> filter(fn: (r) => r.ticker == v.ticker or v.ticker == "")
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded)))
  |> filter(fn: (r) => r._field != "ticker")
  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> keep(columns: [
    "_time", "ticker",
    "volume_weighted_avg_ask_price", "volume_weighted_avg_bid_price",
    "sum_ask_notional", "sum_bid_notional",
    "sum_ask_quantity", "sum_bid_quantity"
  ])

prevQuotes = quotes
  |> timeShift(duration: step)
  |> rename(columns: {
      volume_weighted_avg_bid_price: "prev_vwap_bid",
      volume_weighted_avg_ask_price: "prev_vwap_ask",
      sum_ask_notional: "prev_ask_notional",
      sum_bid_notional: "prev_bid_notional",
      sum_ask_quantity: "prev_ask_quantity",
      sum_bid_quantity: "prev_bid_quantity",
  })

successiveQuotes = join(
    tables: {cur: quotes, prev: prevQuotes},
    on: ["ticker", "_time"],
  )
  |> filter(fn: (r) => r.volume_weighted_avg_ask_price != 0)
  |> filter(fn: (r) => r.volume_weighted_avg_bid_price != 0)
  |> filter(fn: (r) => r.prev_vwap_ask                 != 0)
  |> filter(fn: (r) => r.prev_vwap_bid                 != 0)
  
trades = from(bucket: "stream-lines-market-data-realtime")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r._measurement == "stock-trade-aggregates-sliding-window")
  |> filter(fn: (r) => r.ticker == v.ticker or v.ticker == "")
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded)))
  |> filter(fn: (r) => r._field != "ticker")
  |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> keep(columns: ["_time", "ticker", "volume_weighted_avg_price", "sum_notional", "sum_quantity"])

metrics = join(
  tables: { t: trades, q: successiveQuotes },
  on: ["_time", "ticker"],
)
  |> map(fn: (r) => {
    mid = (r.volume_weighted_avg_bid_price + r.volume_weighted_avg_ask_price) / 2.0

    spreadRate = (r.volume_weighted_avg_ask_price - r.volume_weighted_avg_bid_price)
               / (r.volume_weighted_avg_ask_price + r.volume_weighted_avg_bid_price) * 100.0

    spreadRateWithFloor = if spreadRate < 0.01 then 0.01 else spreadRate

    prevSpreadRate = (r.prev_vwap_ask - r.prev_vwap_bid)
                   / (r.prev_vwap_ask + r.prev_vwap_bid) * 100.0

    askChangeRate = (r.prev_vwap_ask - r.volume_weighted_avg_ask_price)
                  / (r.prev_vwap_ask + r.volume_weighted_avg_ask_price) * 100.0

    bidChangeRate = (r.volume_weighted_avg_bid_price - r.prev_vwap_bid)
                  / (r.volume_weighted_avg_bid_price + r.prev_vwap_bid) * 100.0

    sumQuoteNotional = r.sum_bid_notional + r.sum_ask_notional

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
          (r.sum_bid_notional - r.sum_ask_notional) / sumQuoteNotional
        else
          0.0,

      midChange: 0.5 * (bidChangeRate - askChangeRate),

      //          >         0          <
      //   Spread narrows   |     Spread widens
      //        Push        |     Retreat, Fear
      // Liquidity improves | Liquidity deteriorates
      spreadChange: prevSpreadRate - spreadRate,

      askNotional: r.sum_ask_notional,
      bidNotional: r.sum_bid_notional,
      tradeNotional: r.sum_notional,
      askQuantity: r.sum_ask_quantity,
      bidQuantity: r.sum_bid_quantity,
      tradeQuantity: r.sum_quantity,
  
      //            >           |     0    |           <            | 
      //   VWAP closer to ask   | balanced |  VWAP closer to bid    |
      // Seller-initiated trade |          | Buyer-initiated trade  |
      //   Aggressive seller    |          |    Aggressive buyer    |
      tradePosition:
        if spreadRate > 0.0 then
            (r.volume_weighted_avg_price - mid)
          / (r.volume_weighted_avg_price + mid) * 100.0
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
