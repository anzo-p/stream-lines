<p align="center">
  <img src="doc/images/ripples.png">
Image generated with DALL-E by OpenAI
</p>

# Ripples

An Apache Flink app in Scala to read realtime market data from AWS Kinesis into windows with various computational fields for deeper Microstructure analysis.

Upon launch, will redo entire set of market data for today, if there is any.

### Obtain a runnable package for local testing
```
sbt reload
sbt clean
sbt compile
sbt package
sbt universal:packageBin
```
