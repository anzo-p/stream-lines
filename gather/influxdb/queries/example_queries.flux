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
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")
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
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")
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
start = -48mo // time(v: "2016-01-01"), -60mo

baseQuery = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: now())
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")

aggregateField = (table, field, fn, alias) => table
  |> filter(fn: (r) => r._field == field)
  |> keep(columns: ["_time", "_value", "_field"])
  |> aggregateWindow(every: 1w, fn: fn, createEmpty: false)
  |> map(fn: (r) => ({ r with _field: alias, _value: r._value }))

union(tables: [
  aggregateField(table: baseQuery, field: "priceChangeLow", fn: min, alias: "a_low"),
  aggregateField(table: baseQuery, field: "priceChangeHigh", fn: max,  alias: "b_high"),
  aggregateField(table: baseQuery, field: "priceChangeAvg", fn: mean, alias: "c_avg")
])


// charts the daily and weekly change in index value
// early visual insight; "it will fluctuate" - attractive to place offers for intraday moves with leveraged etf's
start = -60mo // time(v: "2016-01-05"), -60mo

base_filter = (r) =>
  r._measurement == "index-daily-change-regular-hours"
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


// trend of total volume traded on index
// early insights:
// - the dollar value keeps going up even though the unit volumes are going down
// - the market as a whole seems to be timing their buys and sells quite like you too
start = -60mo // time(v: "2016-01-01"), -60mo
movingAvg = 33

backShift = (
  multiplier = 1.5 // half of moving avg period + compensation for window aggregate
) => 
  -duration(v: string(v: int(v: float(v: movingAvg) * multiplier)) + "d")

getField = (
  start = start,
  stop = now(),
  measurement = "index-daily-change-regular-hours"
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

volumes = join(
    tables: {
      volume: totalTradingValue,
      price: priceChangeAvg
    },
    on: ["_time"]
)
  |> map(fn: (r) => ({ r with _value: r._value_volume / r._value_price }))
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "volume")

totalDollarValue = getField(field: "totalTradingValue")
  |> aggregateWindow(every: 1w, fn: mean, createEmpty: false)
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "dollarValue")

totalDollarValue
  |> aggregateWindow(every: 4mo, fn: mean, createEmpty: false)
  |> timeShift(duration: backShift())
  |> keep(columns: ["_time", "_value"])
  |> yield(name: "customMovingAverage")

totalDollarValue
  |> keep(columns: ["_time"])
  |> map(fn: (r) => ({ r with _time: now(), _value: 0.0 }))
  |> yield(name: "y_zero_dummy")


// dumping or hoarding index selected range
// unusual highs (lows) or several recent higher highs (lower lows) suggest hoarding (dumping)
start = -13mo
stop = now()

base_filter = (r) =>
  r._measurement == "index-daily-change-regular-hours"
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
  |> filter(fn: (r) => r["_measurement"] == "index-daily-change-regular-hours")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> filter(fn: (r) => r["_time"] > startResults)
  |> filter(fn: (r) => r["_value"] > 1.0)
  |> aggregateWindow(every: 2w, fn: median)
  |> map(fn: (r) => ({ r with daysBetween: (int(v: r._time) - int(v: startValues)) / (24 * 60 * 60 * 1000 * 1000 * 1000) }))
  |> map(fn: (r) => ({ r with yearsBetween: float(v: r.daysBetween) / 365.0 }))
  |> map(fn: (r) => ({ r with _value: (math.pow(x: r._value, y: 1.0 / r.yearsBetween) - 1.0) * 100.0 }))


// charts the logarithmic trend of individual securities
// early visual insight
// - deep underlying growth: almost all companies at least doubled their value in 10 years
// - failing businessses take many years to rebound, if everimport "math"
import "math"

included = []
excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: time(v: "2016-01-01"), stop: now())
  |> filter(fn: (r) => r["_measurement"] == "securities-daily-change-regular-hours")
  |> filter(fn: (r) => r["_field"] == "priceChangeAvg")
  |> aggregateWindow(every: 1w, fn: last, createEmpty: false)
  |> filter(fn: (r) => r._value > 1.0)
  |> filter(fn: (r) => (length(arr: included) == 0 or contains(value: r.ticker, set: included))) // only when not empty
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded))) // o/wise all except these 
  |> keep(columns: ["_time", "_value", "_field", "company", "ticker"])
  |> map(fn: (r) => ({ r with _value: math.log(x: r._value) }))


// charts the daily change in value for individual securities
// early visual insight: interesing things tends to happen when the daily change spreads out
import "math"

included = []
excluded = []
start = -13w
stop = now()

priceChangePercentage = (r) => (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0

from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == ""securities-daily-change-extended-hours")
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
  // simple attribution thorugh optional cumulation
  //|> cumulativeSum(columns: ["_value"])


  // spread the values away from 0.0 with increased force the closer the value is to 0.0, while maintaining overall shape
  // at 10000.0 and 0.01 there is virtually no effect
  |> map(fn: (r) => ({ r with _value: 10000.0 * math.tanh(x: r._value * 0.3) }))


// logarithmic trend of trading volumes for individual securities
import "math"

excluded = []

from(bucket: "stream-lines-daily-bars")
  |> range(start: time(v: "2016-01-01"), stop: now()) // time(v: "2016-01-01"), -60w
  |> filter(fn: (r) => r["_measurement"] == "securities-daily-change-regular-hours")
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
  r._measurement == ""securities-daily-change-extended-hours"
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
start = -48mo // time(v: "2016-01-01"), -60mo
stop = now()

baseQuery = from(bucket: "stream-lines-daily-bars")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r._measurement == "drawdown")

aggregateField = (table, field, fn, alias) => table
  // experiment between, eg. 2d .. 1w
  |> filter(fn: (r) => r._field == field)
  |> keep(columns: ["_time", "_value", "_field"])
  |> aggregateWindow(every: 1w, fn: fn, createEmpty: false)
  |> map(fn: (r) => ({ r with _field: alias, _value: r._value }))

union(tables: [
  aggregateField(table: baseQuery, field: "drawdownLow", fn: min, alias: "a_low"),
  aggregateField(table: baseQuery, field: "drawdownHigh", fn: max,  alias: "b_high"),
  aggregateField(table: baseQuery, field: "drawdownAvg", fn: mean, alias: "c_avg")
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


// charts the daily attribution of each ticker to movement of index
// early insight suggests
// - there are times when members of the index move together as a coherent looking whole
// - and days when companies get an expanded spread of much more individual looking appraisals
// - it appears helpful to give more weight to events where the index itself also moves more
import "math"

included = [] // filters onto these if list is not empty
excluded = [] // included is empty? then all except these
start = -4mo
stop =  now()
indexRelevance = 0.5 // higher values emphasise days when the index itself moves, practical limits: 0.00001 .. 1.0 
indexDirection = "" // { "bull", "bear", "flat", else shows all }
flatLimit = 0.2 // from experience with data yields less measures than "bear" over any indexRelevance

priceChangeRate = (r) => ((r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"]) * 100.0

attribution = (index, member, indexRelevance) =>
  // think: member / f(index, indexRelevance)
  (member * index) / (math.pow(x: index, y: 2.0) + float(v: indexRelevance))

option plotPriceChange = (table=<-) =>
  table
    |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
    |> map(fn: (r) => ({
        _time: r._time,
        ticker: r.ticker,
        company: r.company,
        _value: priceChangeRate(r)
      }))

base = (measurement) =>
  from(bucket: "stream-lines-daily-bars")
    |> range(start: start, stop: stop)
    |> filter(fn: (r) => r["_measurement"] == measurement)
    |> filter(fn: (r) => r["_field"] == "priceChangeAvg" or  r["_field"] == "prevPriceChangeAvg")

index = base(measurement: "index-daily-change-regular-hours")
  |> plotPriceChange()
  |> filter(fn: (r) =>
    // comanies that push or drag the index
    if      indexDirection == "bull" then r._value > float(v: flatLimit)
    // companies that move anyway
    else if indexDirection == "bear" then r._value < float(v: flatLimit * -1.0)
    // companies that pull away when others lag or either follow or lead the lag
    else if indexDirection == "flat" then r._value <= float(v: flatLimit) and r._value >= float(v: flatLimit * -1.0)
    else true
  )

members = base(measurement: "securities-daily-change-regular-hours")
  |> filter(fn: (r) => (length(arr: included) == 0 or     contains(value: r.ticker, set: included))) 
  |> filter(fn: (r) => (length(arr: excluded) == 0 or not contains(value: r.ticker, set: excluded)))
  |> plotPriceChange()

join(
  tables: {
    index: index
      |> keep(columns: ["_time", "_field", "_value"])
      |> rename(columns: {_value: "index"}),
    member: members
      |> keep(columns: ["_time", "_field", "_value", "ticker", "company"])
      |> rename(columns: {_value: "member"})
  },
  on: ["_time"]
)
  |> map(fn: (r) => ({
      _time: r._time,
      ticker: r.ticker,
      company: r.company,
      _value: attribution(index: r.index, member: r.member, indexRelevance: indexRelevance)
    }))
  |> cumulativeSum(columns: ["_value"])



hi low bands näkymä hyötyisi siitä, että mikä on alin hinta viikon siihen astisen ylimmän jälkeen
koska nykyisellään se näyttää laajaa nauhaa vaikkakin alin hinta tapahtuu useimmiten ajallisesti aikaisemmin

yhdistä volyymit ja dollarimäärät. AMAA ei tarvinne
