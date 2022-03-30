package benchmarks

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 3)
@Threads(2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class Jdbc {

  @Param(Array("100000", "5000000"))
  var rowNumber: Int = _

  def generateSql(): String = s"""SELECT
                                |    randomPrintableASCII(128),
                                |    randomPrintableASCII(128),
                                |    randomPrintableASCII(128),
                                |    randomPrintableASCII(128),
                                |    randomPrintableASCII(128),
                                |    toInt32(rand()),
                                |    toInt32(rand()),
                                |    toInt32(rand()),
                                |    toInt32(rand()),
                                |    toInt32(rand())
                                |FROM numbers($rowNumber)""".stripMargin

  @Benchmark
  def fetchAndConsume(bh: Blackhole): Unit = {
    val sql = generateSql()
    var connection: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    try {
      connection =
        DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000")
      stmt = connection.createStatement
      rs = stmt.executeQuery(sql)
      rs.next()
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
