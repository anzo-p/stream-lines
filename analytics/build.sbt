import sbt._

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "analytics",
    version := "0.1",
    Compile / mainClass := Some("com.anzop.FlinkApp")
  )
  .enablePlugins(JavaAppPackaging)
//.enablePlugins(DockerPlugin)

val awsSdkVersion      = "1.12.429"
val apacheFlinkVersion = "1.17.2"
val apacheHttpVersion  = "4.5.14"
val awsKinesisVersion  = "4.0.0-1.16"
val jacksonVersion     = "2.13.4"
val logbackVersion     = "1.4.12"
val slf4jVersion       = "2.0.5"
val typesafeVersion    = "1.4.2"

libraryDependencies ++= Seq(
  "com.typesafe"                   % "config"                              % typesafeVersion,
  "com.amazonaws"                  % "aws-java-sdk-kinesis"                % awsSdkVersion,
  "org.apache.flink"               % "flink-clients"                       % apacheFlinkVersion,
  "org.apache.flink"               % "flink-connector-kinesis"             % awsKinesisVersion,
  "org.apache.flink"               % "flink-connector-aws-kinesis-streams" % awsKinesisVersion,
  "org.apache.flink"               % "flink-statebackend-rocksdb"          % apacheFlinkVersion,
  "org.apache.flink"               % "flink-s3-fs-hadoop"                  % apacheFlinkVersion,
  "org.apache.flink"               %% "flink-streaming-scala"              % apacheFlinkVersion,
  "org.apache.httpcomponents"      % "httpclient"                          % apacheHttpVersion,
  "com.fasterxml.jackson.core"     % "jackson-core"                        % jacksonVersion,
  "com.fasterxml.jackson.core"     % "jackson-databind"                    % jacksonVersion,
  "com.fasterxml.jackson.module"   %% "jackson-module-scala"               % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"             % jacksonVersion,
  "org.slf4j"                      % "slf4j-api"                           % slf4jVersion,
  "ch.qos.logback"                 % "logback-classic"                     % logbackVersion,
  "com.thesamet.scalapb"           %% "scalapb-runtime"                    % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

Compile / PB.protoSources := Seq(file("../protobuf"))
