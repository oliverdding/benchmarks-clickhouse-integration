val sparkVersion = sys.props.getOrElse("SPARK_VERSION", "3.0.0")

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "benchmarks-clickhouse-integration",
    libraryDependencies ++= Seq(
      // clickhouse-scala-client
      "com.crobox.clickhouse" %% "client" % "1.0.9",
      // sttp
      "com.softwaremill.sttp.client3" %% "core" % "3.5.1",
      // jdbc
      "com.github.housepower" % "clickhouse-native-jdbc-shaded" % "2.6.5",
      // test
      "org.scalatest" %% "scalatest" % "3.2.11" % Test,
      // slf4j
      "org.slf4j" % "slf4j-api" % "1.7.36",
      "org.slf4j" % "slf4j-simple" % "1.7.36" % Test
    )
  )
