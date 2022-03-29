package benchmarks

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
class JdbcBenchmark {

  @Param(Array("500000", "10000000"))
  var rowNumber: Int = _

  val sql: String = s"""SELECT
                      |    randomPrintableASCII(128),
                      |    toInt32(rand())
                      |FROM numbers($rowNumber)""".stripMargin

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @Threads(4)
  @Fork(1)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def run(bh: Blackhole): Unit = {
    var connection: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    try {
      connection =
        DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000")
      stmt = connection.createStatement
      rs = stmt.executeQuery(sql)
      while (rs.next()) {
        bh.consume(rs.getString(0))
        bh.consume(rs.getInt(1))
      }
    } finally {
      if (rs != null) {
        rs.close()
      }
      if (stmt != null) {
        stmt.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
  }

}
