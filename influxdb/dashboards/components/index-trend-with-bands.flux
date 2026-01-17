import "math"

fields = ["priceChangeAvg", "priceChangeHigh", "priceChangeLow"]
start = duration(v: v.longTermYears)
fn = if      v.aggFn == "first"  then first
     else if v.aggFn == "last"   then last
     else if v.aggFn == "max"    then max
     else if v.aggFn == "mean"   then mean
     else if v.aggFn == "median" then median
     else if v.aggFn == "min"    then min
     else                             mean

baseQuery = from(bucket: "stream-lines-market-data-historical")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")
  
aggregateField = (table, field, fn, alias) => table
  |> filter(fn: (r) => r._field == field)
  |> keep(columns: ["_time", "_value", "_field"])
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> map(fn: (r) => ({ r with _field: alias, _value: math.log(x: r._value) }))

union(tables: [
  aggregateField(table: baseQuery, field: "priceChangeLow", fn: min, alias: "a_low"),
  aggregateField(table: baseQuery, field: "priceChangeHigh", fn: max,  alias: "b_high"),
  aggregateField(table: baseQuery, field: "priceChangeAvg", fn: fn, alias: "c_avg")
])