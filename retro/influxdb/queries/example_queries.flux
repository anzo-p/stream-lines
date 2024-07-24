// linear index value development over entire span
from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)


// logarithmic trend of index value over entire time range, aggregated
import "math"

fields = ["priceChangeAvg"] //, "priceChangeHigh", "priceChangeLow"]

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> keep(columns: ["_time", "_value", "regularTradingHours"])
  // min, max, first, last, mean, median, sum, count
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "_value")
  |> movingAverage(n: 50)
  |> timeShift(duration: -150d)


// trend of index value over selected time range, with high low bands
import "math"

fields = ["priceChangeAvg", "priceChangeHigh", "priceChangeLow"]

from(bucket: "stream-lines-daily-bars")
  |> range(start: -6mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> keep(columns: ["_time", "_value", "_field", "ticker"])
  // uncomment to toggle logarithmic - linear
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))


// daily change in index value over entire time range
curr = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")

prev = curr
  |> timeShift(duration: 1d)

join(
  tables: {current: curr, previous: prev},
  on: ["_time"],
  method: "inner"
)
  |> map(fn: (r) => ({
      _time: r._time,
      _value: (r._value_current - r._value_previous) / r._value_previous * 100.0,
      _field: "percentageChange"
  }))


// daily change in value over selected time range, also cumulated
curr = from(bucket: "stream-lines-daily-bars")
  |> range(start: -6mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")

prev = curr
  |> timeShift(duration: 1d)

join(
  tables: {current: curr, previous: prev},
  on: ["_time"],
  method: "inner"
)
  |> map(fn: (r) => ({
      _time: r._time,
      _value: (r._value_current - r._value_previous) / r._value_previous * 100.0,
      _field: "percentageChange"
  }))
  |> yield(name: "dailyChange")
  |> cumulativeSum(columns: ["_value"])


// trend of total trading values on index
from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> keep(columns: ["_time", "_value", "_field", "measurement", "ticker", "regularTradingHours"])
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "_value")
  |> movingAverage(n: 50)
  |> timeShift(duration: -150d)


// trend of normalized trading volumes over entire time range, aggregated
dailyTradingValue = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "totalTradingValue")
  |> keep(columns: ["_time", "_value"])

dailyPriceChange = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "priceChangeAvg")
  |> keep(columns: ["_time", "_value"])

normalizedTradingVolume = join(
    tables: {volume: dailyTradingValue, price: dailyPriceChange},
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "result")


// trend of normalized trading volumes over selected time range
dailyTradingValue = from(bucket: "stream-lines-daily-bars")
  |> range(start: -6mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "totalTradingValue")
  |> keep(columns: ["_time", "_value"])

dailyPriceChange = from(bucket: "stream-lines-daily-bars")
  |> range(start: -6mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "priceChangeAvg")
  |> keep(columns: ["_time", "_value"])

normalizedTradingVolume = join(
    tables: {volume: dailyTradingValue, price: dailyPriceChange},
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))
  |> yield(name: "result")


// daily annualized rate of gain - set y Column to "gain"
import "math"

startValues = time(v: "2016-01-01")
startResults = time(v: "2017-01-01")

from(bucket: "stream-lines-daily-bars")
  |> range(start: startResults, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => r["_time"] > startResults)
  |> aggregateWindow(every: 2w, fn: median, createEmpty: false)
  |> filter(fn: (r) => r["_value"] > 1.0)
  |> map(fn: (r) => ({
    r with
    days: (int(v: r._time) - int(v: startValues)) / (24 * 60 * 60 * 1000000000),
  }))
  |> map(fn: (r) => ({
    r with
    years: float(v: r.days) / 365.0
  }))
  |> map(fn: (r) => ({
    r with
    gain: (math.pow(x: r._value, y: 1.0 / r.years) - 1.0) * 100.0
  }))
  |> keep(columns: ["_time", "gain"])
  |> yield(name: "gain")


// logarithmic trend of individual stocks
import "math"

excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  // optionally filter only for stocks that actually gained value
  |> filter(fn: (r) => r._value > 1.0)
  |> aggregateWindow(every: v.windowPeriod, fn: mean, createEmpty: false)
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "mean")


// daily change in value for individual securities over selected time range
curr = from(bucket: "stream-lines-daily-bars")
  |> range(start: -2w, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeDaily")
  |> keep(columns: ["_time", "ticker", "company", "_value"])

prev = curr
  |> timeShift(duration: 1d)

join(
  tables: {current: curr, previous: prev},
  on: ["_time", "ticker", "company"],
  method: "inner"
)
  |> map(fn: (r) => ({
      r with
      _value: (r._value_current - r._value_previous) / r._value_previous * 100.0,
  }))


// trend of total trading values on individual stocks
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "mean")
