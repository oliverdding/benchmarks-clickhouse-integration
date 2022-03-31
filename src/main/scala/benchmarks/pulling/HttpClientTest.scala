package benchmarks.pulling

import com.clickhouse.client._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 4, time = 3)
@Measurement(iterations = 5, time = 3)
@Threads(2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class HttpClientTest {

  @Param(Array("100000", "5000000"))
  var rowNumber: Int = _

  @Param(Array("RowBinary"))
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
            _ <: ClickHouseRequest[_ <: ClickHouseRequest[_]]
          ]
        ]]
    val response: ClickHouseResponse = request
      .compressServerResponse(true)
      .format(ClickHouseFormat.RowBinary) // TODO: If you want to add format, here is a trap
      .query(sql)
      .execute()
      .get()

    var cnt = 0
    response
      .records()
      .forEach(r =>
        r.forEach(v => {
          cnt += 1
          bh.consume(v)
        })
      )
    println(s"TAG ${cnt}")

    client.close()
  }
}
