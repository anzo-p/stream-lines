included = []
excluded = [v.exclTicker1, v.exclTicker2, v.exclTicker3]

start = duration(v: v.shortTermRange)

trades = from(bucket: "stream-lines-market-data-realtime")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r._measurement == "stock-trade-aggregates-sliding-window")
  |> filter(fn: (r) => r._field == "sum_notional")
  |> filter(fn: (r) => (length(arr: included) == 0 or     contains(value: r.ticker, set: included)))
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded)))
  |> keep(columns: ["_time", "_value", "ticker"])
  |> yield(name: "Notional")

trades
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 0.0 }))
  |> yield(name: "0")
