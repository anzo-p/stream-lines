import "math"

excluded = []
start = duration(v: v.longTermYears)
fn = if      v.aggFn == "first"  then first
     else if v.aggFn == "last"   then last
     else if v.aggFn == "max"    then max
     else if v.aggFn == "mean"   then mean
     else if v.aggFn == "median" then median
     else if v.aggFn == "min"    then min
     else                             mean

from(bucket: "stream-lines-market-data-historical")
  |> range(start: start, stop: now()) // time(v: "2016-01-01"), -60w
  |> filter(fn: (r) => r["_measurement"] == "securities-daily-change-regular-hours")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> keep(columns: ["_time", "_field", "_value", "company", "ticker"])
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  //|> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "last")