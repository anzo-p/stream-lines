import "dict"

excluded = [v.not1, v.not2, v.not3]
start    = duration(v: v.rangeD)

condAggregate = (tables=<-) =>
  if      v.rangeD == "-2d" then tables |> aggregateWindow(every: duration(v: string(v: 2 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-3d" then tables |> aggregateWindow(every: duration(v: string(v: 3 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-4d" then tables |> aggregateWindow(every: duration(v: string(v: 4 * 3) + "m"), fn: mean, createEmpty: false)  
  else if v.rangeD == "-7d" then tables |> aggregateWindow(every: duration(v: string(v: 5 * 3) + "m"), fn: mean, createEmpty: false)  
  else                           tables

base = (measurement, fieldRegex) =>
  from(bucket: "stream-lines-market-data-realtime")
    |> range(start: start, stop: now())
    |> filter(fn: (r) => r._measurement == measurement)
    |> filter(fn: (r) => r._field =~ fieldRegex)
    |> filter(fn: (r) => r.ticker == v.ticker or v.ticker == "")
    |> filter(fn: (r) => length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))
    |> group(columns: ["ticker", "_field"])
    |> keep(columns: ["_time", "_field", "_value", "ticker"])

pctChange = (tables=<-) => {
  open = tables
    |> first()
    |> rename(columns: {_value: "open"})
    |> keep(columns: ["ticker", "_field", "open"])

  return join(tables: {t: tables, o: open}, on: ["ticker", "_field"])
    |> filter(fn: (r) => exists r._value and r._value != 0.0)
    |> map(fn: (r) => ({
      _time: r._time,
      ticker: r.ticker,
      _field: r._field,
      _value: (r._value - r.open) / r.open * 100.0
    }))
}

// !nb knows nothing that would require understanding semantics behind those arguments
makeTab = (measurement, fieldRegex, fieldRenames) =>
  base(
    measurement: measurement,
    fieldRegex: fieldRegex
  )
  |> filter(fn: (r) => r._value != 0.0 or not exists r._value) // if exists, must be nonzero
  |> condAggregate()
  |> map(fn: (r) => ({
    r with
    _field: dict.get(
      dict: fieldRenames,
      key: r._field,
      default: r._field
    ),
    _value: float(v: r._value),
  }))
    |> pctChange()

wide = union(tables: [
  makeTab(
    measurement: "stock-quotation-aggregates-sliding-window",
    fieldRegex: /^(volume_weighted_avg_ask_price|volume_weighted_avg_bid_price)$/,
    fieldRenames: [
      "volume_weighted_avg_ask_price": "vwap_ask",
      "volume_weighted_avg_bid_price": "vwap_bid"
    ]
  ),
  makeTab(
    measurement: "stock-trade-aggregates-sliding-window",
    fieldRegex: /^volume_weighted_avg_price$/,
    fieldRenames: [
      "volume_weighted_avg_price": "vwap_price"
    ]
  )
])
  |> group(columns: ["ticker"])
  |> pivot(rowKey: ["_time", "ticker"], columnKey: ["_field"], valueColumn: "_value")
  |> filter(fn: (r) => exists r.vwap_ask and exists r.vwap_bid and exists r.vwap_price)
  |> sort(columns: ["_time"])

delta = wide
  |> group(columns: ["ticker"])
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

result = withDelta
  |> map(fn: (r) => ({
    r with
    askChange: r.prev_vwap_ask - r.vwap_ask,
    bidChange: r.vwap_bid      - r.prev_vwap_bid,
    spread:    r.vwap_ask      - r.vwap_bid,
    mid:      (r.vwap_bid      + r.vwap_ask)      / 2.0,
  }))
  |> keep(columns: [
    "_time", "ticker",
    "vwap_ask", "vwap_bid", "vwap_price",
    "prev_vwap_ask", "prev_vwap_bid", "prev_vwap_price",
    "askChange", "bidChange", "spread", "mid"
  ])

yValue = (tables=<-, name, valueFn, cumulative = false) =>
  (if bool(v: v.cumul) and cumulative then
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

if      v.primit == "askChange" then result |> yValue(name: "askChange",  valueFn: (r) => r.askChange, cumulative: true)
else if v.primit == "bidChange" then result |> yValue(name: "bidChange",  valueFn: (r) => r.bidChange, cumulative: true)
else if v.primit == "spread"    then result |> yValue(name: "spread",     valueFn: (r) => r.spread,    cumulative: true)
else if v.primit == "mid"       then result |> yValue(name: "mid",        valueFn: (r) => r.mid)
else if v.primit == "vwapAsk"   then result |> yValue(name: "vwap_ask",   valueFn: (r) => r.vwap_ask)
else if v.primit == "vwapBid"   then result |> yValue(name: "vwap_bid",   valueFn: (r) => r.vwap_bid)
else if v.primit == "vwapPrice" then result |> yValue(name: "vwap_price", valueFn: (r) => r.vwap_price)
else                                 result |> yValue(name: "vwap_price", valueFn: (r) => r.prev_vwap_price)
