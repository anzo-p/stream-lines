import sbt._

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "ripples",
    version := "0.1",
    Compile / mainClass := Some("net.anzop.FlinkApp")
  )
  .enablePlugins(JavaAppPackaging)
//.enablePlugins(DockerPlugin)

val apacheFlinkVersion = "1.17.2"
val awsSdkVersion      = "1.12.429"
val awsKinesisVersion  = "4.0.0-1.16"
val jacksonVersion     = "2.15.3"
val logbackVersion     = "1.4.12"
val slf4jVersion       = "2.0.5"
val typesafeVersion    = "1.4.2"

libraryDependencies ++= Seq(
  "com.amazonaws"        % "aws-java-sdk-kinesis"                % awsSdkVersion,
  "org.apache.flink"     % "flink-clients"                       % apacheFlinkVersion,
  "org.apache.flink"     % "flink-connector-base"                % apacheFlinkVersion,
  "org.apache.flink"     % "flink-connector-kinesis"             % awsKinesisVersion,
  "org.apache.flink"     % "flink-connector-aws-kinesis-streams" % awsKinesisVersion,
  "org.apache.flink"     % "flink-statebackend-rocksdb"          % apacheFlinkVersion,
  "org.apache.flink"     % "flink-s3-fs-hadoop"                  % apacheFlinkVersion,
  "org.apache.flink"     %% "flink-streaming-scala"              % apacheFlinkVersion,
  "org.apache.flink"     %% "flink-scala"                        % apacheFlinkVersion % Provided,
  "org.apache.flink"     % "flink-streaming-java"                % apacheFlinkVersion % Provided,
  "org.slf4j"            % "slf4j-api"                           % slf4jVersion,
  "ch.qos.logback"       % "logback-classic"                     % logbackVersion,
  "com.typesafe"         % "config"                              % typesafeVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime"                    % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

libraryDependencies ++= Seq(
  "org.apache.flink" % "flink-test-utils" % apacheFlinkVersion % Test,
  "org.scalatest"    %% "scalatest"       % "3.2.18"           % "test"
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

Compile / PB.protoSources := Seq(file("../protobuf"))
