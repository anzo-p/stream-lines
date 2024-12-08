import sbt._

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "currents",
    version := "0.1",
    Compile / mainClass := Some("net.anzop.Currents")
  )
  .enablePlugins(JavaAppPackaging)
//.enablePlugins(DockerPlugin)

val apacheFlinkVersion = "1.17.2"
val apacheHttpVersion  = "4.5.14"
val jacksonVersion     = "2.13.4"
val logbackVersion     = "1.4.12"
val slf4jVersion       = "2.0.5"
val typesafeVersion    = "1.4.2"

libraryDependencies ++= Seq(
  "com.typesafe"                   % "config"                     % typesafeVersion,
  "org.apache.flink"               % "flink-clients"              % apacheFlinkVersion,
  "org.apache.flink"               % "flink-statebackend-rocksdb" % apacheFlinkVersion,
  "org.apache.flink"               % "flink-s3-fs-hadoop"         % apacheFlinkVersion,
  "org.apache.flink"               %% "flink-streaming-scala"     % apacheFlinkVersion,
  "org.apache.httpcomponents"      % "httpclient"                 % apacheHttpVersion,
  "com.influxdb"                   % "influxdb-client-java"       % "6.9.0",
  "com.fasterxml.jackson.core"     % "jackson-core"               % jacksonVersion,
  "com.fasterxml.jackson.core"     % "jackson-databind"           % jacksonVersion,
  "com.fasterxml.jackson.module"   %% "jackson-module-scala"      % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"    % jacksonVersion,
  "org.scala-lang.modules"         %% "scala-collection-compat"   % "2.11.0",
  "org.scalanlp"                   %% "breeze"                    % "2.1.0",
  "org.slf4j"                      % "slf4j-api"                  % slf4jVersion,
  "ch.qos.logback"                 % "logback-classic"            % logbackVersion
)

libraryDependencies ++= Seq(
  "org.apache.flink" % "flink-test-utils" % apacheFlinkVersion % Test,
  "org.scalatest"    %% "scalatest"       % "3.2.18"           % "test"
)
