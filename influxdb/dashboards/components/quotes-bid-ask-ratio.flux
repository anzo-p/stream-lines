import "math"

start = duration(v: v.shortTermRange)

base = from(bucket: "stream-lines-market-data-realtime")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r._measurement == "stock-quotation-aggregates-sliding-window")
  //|> aggregateWindow(every: 20m, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value", "_field", "ticker"])

ask = base |> filter(fn: (r) => r._field == "sum_ask_notional")
bid = base |> filter(fn: (r) => r._field == "sum_bid_notional")

joined = join(
    tables: {bid: bid, ask: ask},
    on: ["_time", "ticker"]
)
  |> map(fn: (r) => ({
      _time: r._time,
      // positive when more money or activity in buy orders / prices appear demand drives
      _value: math.tanh(x: (r._value_bid - r._value_ask) / (r._value_bid + r._value_ask)),
      symbol: r.symbol,
  }))

bidAskResult =
  if bool(v: v.splitByTickers) then
    joined
  else
    joined
      |> group(columns: ["_time"])
      |> mean(column: "_value")
      |> group()
      |> map(fn: (r) => ({
        r with
        symbol: "BASKET"
      }))

bidAskResult
  |> yield(name: "Bid / Ask")

base
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 1.0 }))
  |> yield(name: "y_top_dummy")

base
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: -1.0 }))
  |> yield(name: "y_bottom_dummy")
