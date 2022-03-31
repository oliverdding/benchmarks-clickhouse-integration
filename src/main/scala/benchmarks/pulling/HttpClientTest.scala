package benchmarks.pulling

import com.clickhouse.client._
import org.apache.log4j.BasicConfigurator
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 3)
@Threads(1)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class HttpClientTest {

  @Param(Array("100000", "5000000"))
  var rowNumber: Int = _

  @Param(Array("RowBinary", "Native"))
  var dataFormat: String = _

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
                                 |FROM numbers($rowNumber)
                                 |FORMAT $dataFormat""".stripMargin

  @Benchmark
  def fetchAndDecompressSync(bh: Blackhole): Unit = {
    val sql = generateSql()
    val client = ClickHouseClient
      .builder()
      .defaultCredentials(
        ClickHouseCredentials.fromUserAndPassword("default", "")
      )
      .build()
    val server = ClickHouseNode
      .builder()
      .host("127.0.0.1")
      .port(ClickHouseProtocol.HTTP, 8123)
      .build()
    val request =
      client
        .connect(server)
        .asInstanceOf[ClickHouseRequest[
          _ <: ClickHouseRequest[
            _ <: ClickHouseRequest[_]
          ]
        ]]
    val response: ClickHouseResponse = request
      .query(sql)
      .execute()
      .get()

    response.records().forEach(r => bh.consume(r))

    client.close()
  }
}
