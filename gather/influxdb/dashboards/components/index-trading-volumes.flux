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
  multiplier = 0.2 // half of moving avg period + compensation for window aggregate
) => 
  -duration(v: string(v: int(v: float(v: movingAvg) * multiplier)) + "d")

getField = (
  start = start,
  stop = now(),
  measurement = "index-daily-change-regular-hours"
  field,
) => 
  from(bucket: "stream-lines-market-data-historical")
    |> range(start: start, stop: stop)
    |> filter(fn: (r) => r._measurement == measurement)
    |> filter(fn: (r) => r._field == field)

totalTradingValue = getField(field: "totalTradingValue")

priceChangeAvg = getField(field: "priceChangeAvg")

join(
    tables: {
      volume: totalTradingValue,
      price: priceChangeAvg
    },
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "volume")

totalTradingValue
  |> aggregateWindow(every: duration(v: v.aggDays), fn: fn, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "dollarValue")

totalTradingValue
  |> aggregateWindow(
        every: duration(v: string(v: int(v: movingAvg)) + "d"),
        fn: mean,
        createEmpty: false
     )
  |> timeShift(duration: backShift())
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "customMovingAverage")

totalTradingValue
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 0.0 }))
  |> yield(name: "y_zero_dummy")
