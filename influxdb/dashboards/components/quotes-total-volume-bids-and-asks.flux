excluded = [v.exclTicker1, v.exclTicker2, v.exclTicker3]

start = duration(v: v.shortTermRange)

base = from(bucket: "stream-lines-market-data-realtime")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r._measurement == "stock-quotation-aggregates-sliding-window")
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.symbol, set: excluded))) 
  |> keep(columns: ["_time", "_value", "_field", "symbol"])

agg = (field) => {
  result = base |> filter(fn: (r) => r._field == field)

  return if bool(v: v.splitByTickers) then
    result
      |> group(columns: ["ticker"])
  else
    result
      |> group(columns: ["_time"])
      |> sum(column: "_value")
      |> group(columns: ["_field"])
}

agg(field: "sum_ask_notional") |> yield(name: "Asks total")
agg(field: "sum_bid_notional") |> yield(name: "Bids total")

base
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 1.0 }))
  |> yield(name: "y_zero_dummy")
