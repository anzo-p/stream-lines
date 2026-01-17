import "math"

start = duration(v: v.longTermYears)
movingAvg = int(v: v.mAvgDays)
fn = if      v.aggFn == "first"  then first
     else if v.aggFn == "last"   then last
     else if v.aggFn == "max"    then max
     else if v.aggFn == "mean"   then mean
     else if v.aggFn == "median" then median
     else if v.aggFn == "min"    then min
     else                             mean

backShift = (
  multiplier = 0.5 // half of moving avg period + compensation for window aggregate
) => 
  -duration(v: string(v: int(v: float(v: movingAvg) * multiplier)) + "d")

base = from(bucket: "stream-lines-market-data-historical")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> keep(columns: ["_time", "_value", "result"])

base
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> keep(columns: ["_time", "_value", "regularTradingHours"])
  |> yield(name: "priceChangeAvg")

base
  |> movingAverage(n: movingAvg)
  |> timeShift(duration: backShift())
  |> yield(name: "movingAverage")

base
  // kaufman moving avg reactes faster, ie. much less lag
  |> kaufmansAMA(n: int(v: movingAvg))
  |> timeShift(duration: backShift(multiplier: 0.15))
  |> yield(name: "kaufmanAMA")