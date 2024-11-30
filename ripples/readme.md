moving average, moving volume
bid ask imbalance
correlation
alert unusual events
maintain sell and buy bands

do the results and smartly store in influxdb
push to stream for backend only once the backend exists, and maybe only once the dashboard also exists

we will need historical data

- daily opening, min, max, and close, total volume
- should provide us with data to compute trends against which to compare current data
- we should choose the right database and once it is populated we should store and backup the disk separately from launching and tearing down the architecture

but we should experiment with current first, and only assume that we will later have better trend data

### Notes

- explicit functions
- new MapFunction
- new FlatMapFunction
- new ProcessFunction
- new ReduceFunction

---

- Keyed streams
- Windowed streams with watermarks
- tumbling, sliding, session windows

---

- unioning
- window joins
- interval joins
- connect

Better than sliding windows should

- take a pair of tumbling windows
- overlapping half and half
- zipping pairwise in their chronological order
- remove one by taking average to its immediate pred and succ
- use soe analytical algo to smooth it out

------

```
sbt reload
sbt clean
sbt compile
sbt package
sbt universal:packageBin
```
