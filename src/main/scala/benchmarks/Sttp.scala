package benchmarks
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import sttp.client3._

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 3)
@Threads(2)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class Sttp {

  @Param(Array("1000000", "100000000"))
  var rowNumber: Int = _

  @Param(Array("ArrowStream", "Parquet"))
  var dataFormat: String = _

  @Param(Array("0", "1"))
  var compressionEnabled: String = _

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
  def fetchAndConsumeZipSync(bh: Blackhole): Unit = {
    val sql = generateSql()
    lazy val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
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
    backend.close()
  }
}
