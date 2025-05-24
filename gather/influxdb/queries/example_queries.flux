//
// Performance guidelines
// 1. filter raw data - time, measurement, fields
// 2. aggregate as soon as feasible, not sooner
// 3. pivot as soon as feasible, though after aggregate
// 3. filter, map, pivot only upon aggregated results if possible
//

// linear index value development over entire span
from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)


// logarithmic trend of index value over entire time range, aggregated
import "math"

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> keep(columns: ["_time", "_value", "regularTradingHours"])
  // min, max, first, last, mean, median, sum, count
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "_value")
  |> movingAverage(n: 50)
  |> timeShift(duration: -150d)


// trend of index value over selected time range, with high low bands
import "math"

fields = ["priceChangeAvg", "priceChangeHigh", "priceChangeLow"]
start = -48mo

// the prefixes provides alphabetical sorting that tends to more pleasing colors in influxdb data explorer
sortForColors = (r) =>
  if r._field == "priceChangeAvg" then "c_price_change_avg"
  else if r._field == "priceChangeHigh" then "b_price_change_high"
  else if r._field == "priceChangeLow" then "a_price_change_low"
  else r._field

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> aggregateWindow(every: v.windowPeriod, fn: min, createEmpty: false)
  |> keep(columns: ["_time", "_value", "_field"])
  |> map(fn: (r) => ({
    r with 
    _value: math.log(x: r._value),
    _field: sortForColors(r)
  }))


// daily change in index value
start = -7mo
// time(v: "2016-01-05") // for beginning plus a week

base_filter = (r) =>
  r._measurement == "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
  and (r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")

priceChangePercentage = (r) => (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      _value: priceChangePercentage(r)
  }))


// trend of total trading value on index
import "experimental"

data = from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: now())
  |> filter(fn: (r) =>
        r._measurement == "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
        and r._field == "totalTradingValue"
      )

granular = data
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "tradingValue")

smooth = data
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: -6w)
  |> filter(fn: (r) => r._time < experimental.addDuration(d: -6w, to: now()))
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "movingAverage")


// trend of normalized trading volumes over entire time range
measurement = "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d

getField = (field) => 
  from(bucket: "stream-lines-daily-bars")
    |> range(start: v.timeRangeStart, stop: now())
    |> filter(fn: (r) => r["_measurement"] == measurement)
    |> filter(fn: (r) => r._field == field)
    |> aggregateWindow(every: 1w, fn: mean)
    |> keep(columns: ["_time", "_value"])

join(
    tables: {
      volume: getField(field: "totalTradingValue"),
      price: getField(field: "priceChangeAvg"),
    },
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))


// trend of normalized trading volumes over selected time range
import "experimental"

start = -48mo
stop = now()

getField = (field) => 
  from(bucket: "stream-lines-daily-bars")
    |> range(start: start, stop: stop)
    |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
    |> filter(fn: (r) => r._field == field)

data = join(
    tables: {
      volume: getField(field: "totalTradingValue"),
      price: getField(field: "priceChangeAvg")
    },
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))

data
  |> aggregateWindow(every: v.windowPeriod, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "volume")

data
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: -6w) // 6w to 4mo makes a nice visual symmetry
  |> filter(fn: (r) => r._time < experimental.addDuration(d: -6w, to: now()))
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "ma")


// dumping or hoarding index selected range
// unusual highs (lows) or several recent higher highs (lower lows) suggest hoarding (dumping)
start = -13mo
stop = now()

base_filter = (r) =>
  r._measurement == "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
  and (r._field == "priceChangeAvg" or r._field == "prevPriceChangeAvg" or r._field == "totalTradingValue")

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      r with
      ticker: r.ticker,
      company: r.company,
      _value: if r["priceChangeAvg"] > r["prevPriceChangeAvg"] then r["totalTradingValue"] else r["totalTradingValue"] * -1.0
    }))


// daily annualized rate of gain
import "math"

startValues = time(v: "2016-01-01")
startResults = time(v: "2017-01-01")

from(bucket: "stream-lines-daily-bars")
  |> range(start: startResults, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => r["_time"] > startResults)
  |> filter(fn: (r) => r["_value"] > 1.0)
  |> aggregateWindow(every: 2w, fn: median)
  |> map(fn: (r) => ({ r with daysBetween: (int(v: r._time) - int(v: startValues)) / (24 * 60 * 60 * 1000 * 1000 * 1000) }))
  |> map(fn: (r) => ({ r with yearsBetween: float(v: r.daysBetween) / 365.0 }))
  |> map(fn: (r) => ({ r with _value: (math.pow(x: r._value, y: 1.0 / r.yearsBetween) - 1.0) * 100.0 }))


// logarithmic trend of individual stocks
import "math"

included = []
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_reg_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: v.windowPeriod, fn: last, createEmpty: false)
  |> filter(fn: (r) => r._value > 1.0)
  |> filter(fn: (r) => (length(arr: included) == 0 or contains(value: r.ticker, set: included))) // only when not empty
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))) // o/wise all except these 
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))


// logarithmic trend of selected securities for selected time range
import "math"

included = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: -13mo, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_xh_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => (length(arr: included) == 0 or contains(value: r.ticker, set: included))) // only when not empty
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))


// daily change in value for individual securities over selected time range
included = []
excluded = []
start = -11w
stop = now()

priceChangePercentage = (r) => (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "sec_xh_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")
  |> filter(fn: (r) => (length(arr: included) == 0 or contains(value: r.ticker, set: included))) // only when not empty
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))) // o/wise all except these 
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      ticker: r.ticker,
      company: r.company,
      _value: priceChangePercentage(r)
    }))


// trend of total trading values on individual stocks over entire time range
import "math"

excluded = [] //["AAPL", "AMD", "AMZN", "BA", "META", "MSFT", "NFLX", "NVDA", "TSLA"]

from(bucket: "stream-lines-daily-bars")
  |> range(start: v.timeRangeStart, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_reg_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  //|> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "last")


// trend of total trading values on individual stocks over selected time range
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: -5w, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_reg_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))


// dumping or hoarding? individual securities, selected range
included = [] 
excluded = [] 
start = -13w
stop = now()

isEmpty = (arr) => length(arr) == 0

base_filter = (r) =>
  r._measurement == "sec_xh_arith_d" // sec_reg_arith_d, sec_xh_arith_d
  and (r._field == "priceChangeAvg" or r._field == "prevPriceChangeAvg" or r._field == "totalTradingValue")
  and (length(arr: included) == 0 or contains(value: r.ticker, set: included)) // only when not empty
  and (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded)) // o/wise all except these 

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      r with
      ticker: r.ticker,
      company: r.company,
      _value: if r["priceChangeAvg"] > r["prevPriceChangeAvg"] then r["totalTradingValue"] else r["totalTradingValue"] * -1.0
    }))
  |> keep(columns: ["_time", "_value", "ticker", "company"])


// drawdown on index as windowed in min, mean, and max over Avg Price Change
start = -48mo
stop = now()

baseQuery = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "drawdown")
  |> filter(fn: (r) => r["_field"] == "drawdown")
  |> keep(columns: ["_time", "_value", "_field"])

aggregateField = (table, fn, alias) => 
  table
    |> aggregateWindow(every: v.windowPeriod, fn: fn, createEmpty: false)
    |> map(fn: (r) => ({ r with _field: alias, _value: r._value }))

union(tables: [
  aggregateField(table: baseQuery, fn: min, alias: "a_drawdown_min"),
  aggregateField(table: baseQuery, fn: max, alias: "b_drawdown_max"),
  aggregateField(table: baseQuery, fn: mean, alias: "c_drawdown_mean")
])


// trend analysis
fields = ["regression_slope", "regression_variance", "growth"]
start = -48mo

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "trend")
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> aggregateWindow(every: v.windowPeriod, fn: mean, createEmpty: false)
  |> yield(name: "mean")
