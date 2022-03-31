val sparkVersion = sys.props.getOrElse("SPARK_VERSION", "3.0.0")

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "benchmarks-clickhouse-integration",
    libraryDependencies ++= Seq(
      // clickhouse-http-client
      "com.clickhouse" % "clickhouse-http-client" % "0.3.2-patch7",
      // clickhouse-scala-client
      "com.crobox.clickhouse" %% "client" % "1.0.9",
      // sttp
      "com.softwaremill.sttp.client3" %% "core" % "3.5.1",
      // third party jdbc
      "com.github.housepower" % "clickhouse-native-jdbc-shaded" % "2.6.5",
      // test
      "org.scalatest" %% "scalatest" % "3.2.11" % Test,
      // Spark
      "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
      "org.apache.spark" %% "spark-sql" % sparkVersion % Provided
    )
  )
