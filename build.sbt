val sparkVersion = sys.props.getOrElse("SPARK_VERSION", "3.0.0")

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "benchmarks-clickhouse-integration",
    libraryDependencies ++= Seq(
      // JDBC
      "com.github.housepower" % "clickhouse-native-jdbc-shaded" % "2.6.5"
    )
  )
