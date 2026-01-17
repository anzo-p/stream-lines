fields = ["regression_variance", "growth"] //, "regression_slope"]
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
  |> filter(fn: (r) => r["_measurement"] == "trends-by-statistical-regression")
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> aggregateWindow(every: 1w, fn: fn, createEmpty: false)
  |> yield(name: "mean")
