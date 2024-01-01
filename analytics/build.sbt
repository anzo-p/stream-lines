name := "control-tower-analytics"

version := "0.1"

scalaVersion := "2.12.15"

val flinkVersion  = "1.13.2"
val awsSdkVersion = "1.12.118"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-clients" % flinkVersion,
  //"org.apache.flink" %% "flink-scala"             % flinkVersion,
  "org.apache.flink"     %% "flink-streaming-scala"   % flinkVersion,
  "org.apache.flink"     %% "flink-connector-kinesis" % flinkVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime"         % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.amazonaws"        % "aws-java-sdk-kinesis"     % awsSdkVersion
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

Compile / PB.protoSources := Seq(file("../protobuf"))
