// linear index value development over entire span
from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)


// logarithmic trend of index value over entire time range, aggregated
import "math"

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
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


// daily change in value over entire time range
base_filter = (r) =>
  r._measurement == "ix_w_eq_d"
  and (r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      _value: (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0,
  }))


// daily change in value over selected time range, also cumulated
start = -6mo
stop = now()

base_filter = (r) =>
  r._measurement == "ix_w_eq_d"
  and (r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      _value: (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0,
  }))


// trend of total trading value on index
hifreq = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "_value")

smooth = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: -6w)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "b")

// moving average would be too jagged
join(
  tables: {a: hifreq, b: smooth},
  on: ["_time"],
  method: "inner"
)


// trend of normalized trading volumes over entire time range
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
  |> aggregateWindow(every: 1w, fn: mean)
  |> yield(name: "result")


// trend of normalized trading volumes over selected time range
start = -12mo
stop = now()

dailyTradingValue = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "totalTradingValue")
  |> keep(columns: ["_time", "_value"])

dailyPriceChange = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r._field == "priceChangeAvg")
  |> keep(columns: ["_time", "_value"])

normalizedTradingVolume = join(
    tables: {volume: dailyTradingValue, price: dailyPriceChange},
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))
  |> yield(name: "result")


// daily annualized rate of gain
import "math"

startValues = time(v: "2016-01-01")
startResults = time(v: "2017-01-01")

from(bucket: "stream-lines-daily-bars")
  |> range(start: startResults, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "ix_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => r["_time"] > startResults)
  |> aggregateWindow(every: 2w, fn: median)
  |> filter(fn: (r) => r["_value"] > 1.0)
  |> map(fn: (r) => ({ r with daysBetween: (int(v: r._time) - int(v: startValues)) / (24 * 60 * 60 * 1000000000) }))
  |> map(fn: (r) => ({ r with yearsBetween: float(v: r.daysBetween) / 365.0 }))
  |> map(fn: (r) => ({ r with _value: (math.pow(x: r._value, y: 1.0 / r.yearsBetween) - 1.0) * 100.0 }))


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


// daily change in value for individual securities over selected time range
from(bucket: "stream-lines-daily-bars")
  |> range(start: -2w, stop: now())
  |> filter(fn: (r) => r._measurement == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      ticker: r.ticker,
      company: r.company,
      _value: (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0,
  }))


// daily change in value for selected securities over selected time range
included = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: -3mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")
  |> filter(fn: (r) => contains(value: r.ticker, set: included))
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      ticker: r.ticker,
      company: r.company,
      _value: (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0
  }))


// trend of total trading values on individual stocks over entire time range
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "sec_w_eq_d")
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "mean")
