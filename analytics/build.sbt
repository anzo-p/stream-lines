name := "control-tower-analytics"

version := "0.1"

scalaVersion := "2.12.15"

val awsSdkVersion = "1.12.118"
val flinkVersion  = "1.13.2"

libraryDependencies ++= Seq(
  "com.amazonaws"             % "aws-java-sdk-kinesis"     % awsSdkVersion,
  "org.apache.flink"          %% "flink-clients"           % flinkVersion,
  "org.apache.flink"          %% "flink-connector-kinesis" % flinkVersion,
  "org.apache.flink"          %% "flink-streaming-scala"   % flinkVersion,
  "org.apache.httpcomponents" % "httpclient"               % "4.5.14",
  "com.thesamet.scalapb"      %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

Compile / PB.protoSources := Seq(file("../protobuf"))
