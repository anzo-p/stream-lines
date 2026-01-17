import "math"

start = duration(v: v.shortTermRange)

data =
  from(bucket: "stream-lines-market-data-realtime")
    |> range(start: start, stop: now())
    |> filter(fn: (r) => r._measurement == "stock-quotation-aggregates-sliding-window")
    |> rename(columns: {symbol: "ticker"})
    |> pivot(
      rowKey:["_time"],
      columnKey:["_field"],
      valueColumn:"_value"
    )

// price deltas per window
withDeltas =
  data
    |> difference(
      columns: ["bid_price_at_window_end", "ask_price_at_window_end"],
      nonNegative: false
    )
    |> rename(columns: {
      bid_price_at_window_end: "dBidPrice",
      ask_price_at_window_end: "dAskPrice"
    })

withIceberg =
  withDeltas
    |> map(fn: (r) => ({
      r with
      icebergIntensity:
        if math.abs(x: r.dBidPrice) < 0.0005 then math.log1p(x: r.sum_bid_volume)
        else                                      0.0
    }))

ip =
  withIceberg
    |> map(fn: (r) => {
      ip = 0.5 * (r.dBidPrice - r.dAskPrice) +
           0.3 * (r.sum_bid_volume - r.sum_ask_volume) +
           0.2 * r.icebergIntensity

      return { r with institutionalPressure: ip, _value: ip }
    })

tickersOrBasket =
  if bool(v: v.allTickers) then
    ip
      // aggregate across tickers per time
      |> group(columns: ["_time"])
      |> mean(column: "_value")
      // now collapse all times into one series (no _time in group key)
      |> group()
      |> map(fn: (r) => ({
        r with
        ticker: "BASKET"
      }))
  else
    ip

tickersOrBasket
  |> keep(columns: ["_time", "_value", "ticker", "institutionalPressure"])
  |> yield(name: "institutionalPressure")
