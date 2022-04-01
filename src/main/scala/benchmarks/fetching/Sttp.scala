package benchmarks.fetching

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import sttp.client3._

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 4, time = 3)
@Measurement(iterations = 5, time = 3)
@Threads(2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class Sttp {

  var backend: SttpBackend[Identity, Any] = _

  @Setup
  def init(): Unit = {
    backend = HttpURLConnectionBackend()
  }

  @TearDown
  def definite(): Unit = {
    backend.close()
  }

  @Param(Array("100000", "1000000"))
  var rowNumber: Int = _

  @Param(
    Array(
      "Arrow",
      "ArrowStream",
      "Parquet",
      "Native",
      "RowBinary"
    )
  )
  var dataFormat: String = _

  @Param(Array("0", "1"))
  var compressionEnabled: String = _

  def generateSql(): String = s"""SELECT
                                |    randomPrintableASCII(10),
                                |    toInt32(rand())
                                |FROM numbers($rowNumber)
                                |FORMAT $dataFormat""".stripMargin

  @Benchmark
  def fetchSync(bh: Blackhole): Unit = {
    val sql = generateSql()
    val response = basicRequest
      .post(
        uri"http://127.0.0.1:8123/?enable_http_compression=${compressionEnabled}"
      )
      .auth
      .basic("default", "")
      .acceptEncoding("gzip, deflate")
      .contentType("text/plain")
      .response(asByteArray)
      .body(sql)
      .send(backend)

    val res = response.body match {
      case Left(x) =>
        throw new Exception(x)
      case Right(x) =>
        x
    }
    bh.consume(res)
  }
}
