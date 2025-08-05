//
// Performance guidelines
// 1. filter raw data - time, measurement, fields
// 2. aggregate as soon as feasible, not sooner
// 3. pivot as soon as feasible, though after aggregate
// 3. filter, map, pivot only upon aggregated results if possible
//

// linear index value development
from(bucket: "stream-lines-daily-bars")
  |> range(start: time(v: "2016-01-01"), stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)


// charts the logarithmic trend of index value
// early visual insight: opportune to sell when exceeding moving avg, particularly Kaufman variant
import "math"

start = time(v: "2016-01-01") // time(v: "2016-01-01"), -60mo
movingAvg = 60 // 60, 33

backShift = (
  multiplier = 1.5 // half of moving avg period + compensation for window aggregate
) => 
  -duration(v: string(v: int(v: float(v: movingAvg) * multiplier)) + "d")

base = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  // min, max, first, last, mean, median, sum, count
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> keep(columns: ["_time", "_value", "regularTradingHours"])

base
  |> yield(name: "priceChangeAvg")

base
  |> movingAverage(n: movingAvg)
  |> timeShift(duration: backShift())
  |> yield(name: "movingAverage")

base
  // kaufman moving avg reactes faster, ie. much less lag
  |> kaufmansAMA(n: int(v: movingAvg))
  |> timeShift(duration: backShift())
  |> yield(name: "kaufmanAMA")


// charts the trend of intraday mean index value with a band of highs and lows
// early visual insight: weekly oscillation already makes room to play, particularly when most in cash
import "math"

fields = ["priceChangeAvg", "priceChangeHigh", "priceChangeLow"]
start = time(v: "2016-01-01") // time(v: "2016-01-01"), -60mo

sortForColors = (r) =>
  // prefixes provides alphabetical sorting for more pleasing colors in influxdb data explorer
  if r._field == "priceChangeAvg" then "c_price_change_avg"
  else if r._field == "priceChangeHigh" then "b_price_change_high"
  else if r._field == "priceChangeLow" then "a_price_change_low"
  else r._field

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d") // ix_reg_arith_d, ix_xh_arith_d
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  // experiment between, eg. 1d .. 1w
  |> aggregateWindow(every: 1w, fn: min, createEmpty: false)
  |> keep(columns: ["_time", "_value", "_field"])
  |> map(fn: (r) => ({
      r with 
      _value: math.log(x: r._value),
      _field: sortForColors(r)
    }))


// charts the daily and weekly change in index value
// early visual insight; "it will fluctuate" - attractive to place offers for intraday moves with leveraged etf's
start = -60mo // time(v: "2016-01-05"), -60mo

base_filter = (r) =>
  r._measurement == "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
  and (r._field == "priceChangeAvg" or r._field == "prevPriceChangeAvg")

priceChangePercentage = (r) => (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0

base = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: base_filter)
  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
  |> map(fn: (r) => ({
      _time: r._time,
      _value: priceChangePercentage(r)
    }))

base
  |> aggregateWindow(every: 1d, fn: mean, createEmpty: false)
  |> yield(name: "a_daily")

base
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "b_weekly")


// charts the trend of total dollar value traded on index
// early visual insight:
// - over time the bound capital tends to go up in value
// - if you buy or sell in batches through the trend, so do many others at much of the same prices
start = -60mo // time(v: "2016-01-01"), -60mo

data = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) =>
        r._measurement == "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
        and r._field == "totalTradingValue"
      )

data
  // try without to see that volume insights naturally imply zero as reference
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 0.0 }))
  |> yield(name: "y_zero_dummy")

data
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "weeklyTradingValue")

data
  |> aggregateWindow(every: 1d, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "dailyTradingValue")

data
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: -6w)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "customMovingAverage")


// trend of total volume traded on index
getField = (
  start = time(v: "2016-01-01"), // time(v: "2016-01-01"), -60mo,
  stop = now(),
  measurement = "ix_reg_arith_d" // ix_reg_arith_d, ix_xh_arith_d
  field,
) => 
  from(bucket: "stream-lines-daily-bars")
    |> range(start: start, stop: stop)
    |> filter(fn: (r) => r._measurement == measurement)
    |> filter(fn: (r) => r._field == field)

totalTradingValue = getField(field: "totalTradingValue")
  |> aggregateWindow(every: 1w, fn: mean)

priceChangeAvg = getField(field: "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: mean)

data = join(
    tables: {
      volume: totalTradingValue,
      price: priceChangeAvg
    },
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))

data
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "volume")

data
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: -6w) // 6w to 4mo makes a nice visual symmetry
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "customMovingAverage")

data
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 0.0 }))
  |> yield(name: "y_zero_dummy")


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


// logarithmic trend of individual securities
import "math"

included = []
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: time(v: "2016-01-01"), stop: now())
  |> filter(fn: (r) => r["_measurement"] == "sec_reg_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> filter(fn: (r) => r._value > 1.0)
  |> filter(fn: (r) => (length(arr: included) == 0 or contains(value: r.ticker, set: included))) // only when not empty
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))) // o/wise all except these 
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))


// charts the daily change in value for individual securities
// early visual insight: interesing things tends to happen when the daily change spreads out
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


// charts the logarithmic trend of individual securities
// earli visual insight
// - deep underlying growth: almost all companies at least doubled their value in 10 years
// - failing businessses take many years to rebound, if ever
import "math"

excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: time(v: "2016-01-01"), stop: now()) // time(v: "2016-01-01"), -60w
  |> filter(fn: (r) => r["_measurement"] == "sec_reg_arith_d") // sec_reg_arith_d, sec_xh_arith_d
  |> filter(fn: (r) => r["_field"] == "totalTradingValue")
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> filter(fn: (r) => not contains(value: r.ticker, set: excluded))
  //|> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))
  |> yield(name: "last")


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


// charts the drawdown on index as windowed in min, mean, and max over Avg Price Change
// early visual insight:
// - dip buying the sp500 appears to work
// - the bottoms tend to be sharp and reboynds swift
// - rarely falls below 20%, altough might
start = time(v: "2016-01-01") // time(v: "2016-01-01"), -60mo
stop = now()

baseQuery = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "drawdown")
  |> filter(fn: (r) => r["_field"] == "drawdown")
  |> keep(columns: ["_time", "_value", "_field"])

aggregateField = (table, fn, alias) => 
  table
    // experiment between, eg. 2d .. 1w
    |> aggregateWindow(every: 1w, fn: fn, createEmpty: false)
    |> map(fn: (r) => ({ r with _field: alias, _value: r._value }))

union(tables: [
  aggregateField(table: baseQuery, fn: min, alias: "a_drawdown_min"),
  aggregateField(table: baseQuery, fn: max, alias: "b_drawdown_max"),
  aggregateField(table: baseQuery, fn: mean, alias: "c_drawdown_mean")
])


// trend analysis
fields = ["regression_slope", "regression_variance", "growth"]
start = time(v: "2016-01-01") // time(v: "2016-01-01"), -60mo

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "trend")
  |> filter(fn: (r) => contains(value: r._field, set: fields))
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> yield(name: "mean")
