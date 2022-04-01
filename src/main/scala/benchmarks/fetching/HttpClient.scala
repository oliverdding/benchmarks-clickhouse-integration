package benchmarks.fetching

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
class HttpClient {

  @Param(Array("100000", "1000000"))
  var rowNumber: Int = _

  @Param(Array("RowBinaryWithNamesAndTypes"))
  var dataFormat: String = _

  @Param(Array("false", "true"))
  var compressionEnabled: Boolean = _

  def generateSql(): String = s"""SELECT
                                 |    randomPrintableASCII(10),
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
      .compressServerResponse(compressionEnabled)
      .format(
        ClickHouseFormat.RowBinaryWithNamesAndTypes
      ) // TODO: If you want to add format, here is a trap
      .query(sql)
      .execute()
      .get()

    val buf =
      new Array[Byte](
        20000000
      ) // TODO: If you want to expand row number, here is a trap
    response.getInputStream.read(buf)
    bh.consume(buf)

    client.close()
  }
}
