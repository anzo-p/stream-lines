package types

import scala.concurrent.duration.Duration

object TimeExtensions {

  implicit class JavaOffsetDateTimeOps(val ts: java.time.OffsetDateTime) extends AnyVal {

    def isRecentUpTo(d: Duration): Boolean =
      java.time.OffsetDateTime.now().minusSeconds(d.toSeconds).isBefore(ts)

    def toProtobufTs: com.google.protobuf.timestamp.Timestamp =
      com.google.protobuf.timestamp.Timestamp {
        ts.toInstant
      }
  }

  implicit class ProtobufTimestampOps(val ts: com.google.protobuf.timestamp.Timestamp) extends AnyVal {

    def toJavaOffsetDateTime: java.time.OffsetDateTime =
      java
        .time
        .OffsetDateTime
        .ofInstant(
          java.time.Instant.ofEpochMilli(ts.seconds * 1000 + ts.nanos / 1000000),
          java.time.ZoneOffset.UTC
        )
  }
}
