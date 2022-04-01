package benchmarks.pulling

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 4, time = 3)
@Measurement(iterations = 5, time = 3)
@Threads(2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class ThirdPartyJdbc {

  @Param(Array("100000", "1000000"))
  var rowNumber: Int = _

  def generateSql(): String = s"""SELECT
                                |    randomPrintableASCII(10),
                                |    toInt32(rand())
                                |FROM numbers($rowNumber)""".stripMargin

  @Benchmark
  def fetch(bh: Blackhole): Unit = {
    val sql = generateSql()
    var connection: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    try {
      connection =
        DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000")
      stmt = connection.createStatement
      stmt.setMaxRows(0)
      rs = stmt.executeQuery(sql)
      while(rs.next()){}
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
