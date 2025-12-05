import "math"

included = [] // filters onto these if list is not empty
excluded = [] // included is empty? then all except these
start = duration(v: v.nearTermMonths)
stop =  now()
indexRelevance = float(v: v.attrIndexRel) // higher values emphasise days when the index itself moves, practical limits: 0.00001 .. 1.0 
indexDirection = v.attrIndexDir // { "bull", "bear", "flat", else shows all }
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
  from(bucket: "stream-lines-market-data-historical")
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
