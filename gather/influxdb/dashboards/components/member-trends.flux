import "math"

included = []
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
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "securities-daily-change-regular-hours")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> filter(fn: (r) => r._value > 0.667)
  |> filter(fn: (r) => (length(arr: included) == 0 or     contains(value: r.ticker, set: included))) // only when not empty
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))) // o/wise all except these 
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))