name := "control-tower-analytics"

version := "0.1"

scalaVersion := "2.12.15"

val awsSdkVersion      = "1.12.118"
val apacheflinkVersion = "1.15.0"
val apacheHttpVersion  = "4.5.14"

libraryDependencies ++= Seq(
  "com.amazonaws"             % "aws-java-sdk-kinesis"       % awsSdkVersion,
  "org.apache.flink"          % "flink-clients"              % apacheflinkVersion,
  "org.apache.flink"          % "flink-connector-kinesis"    % apacheflinkVersion,
  "org.apache.flink"          % "flink-s3-fs-hadoop"         % apacheflinkVersion,
  "org.apache.flink"          % "flink-statebackend-rocksdb" % apacheflinkVersion,
  "org.apache.flink"          %% "flink-streaming-scala"     % apacheflinkVersion,
  "org.apache.httpcomponents" % "httpclient"                 % apacheHttpVersion,
  "com.thesamet.scalapb"      %% "scalapb-runtime"           % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

Compile / PB.protoSources := Seq(file("../protobuf"))
