import "math"

included = []
excluded = []
start = duration(v: v.nearTermMonths)
stop = now()

priceChangePercentage = (r) => (r["priceChangeAvg"] - r["prevPriceChangeAvg"]) / r["prevPriceChangeAvg"] * 100.0

from(bucket: "stream-lines-market-data-historical")
  |> range(start: start, stop: stop)
  |> filter(fn: (r) => r["_measurement"] == "securities-daily-change-regular-hours")
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