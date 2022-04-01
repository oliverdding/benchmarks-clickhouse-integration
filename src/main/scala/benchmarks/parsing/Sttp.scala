package benchmarks.parsing

import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.ipc.ArrowStreamReader
import org.apache.spark.sql.vectorized.{ArrowColumnVector, ColumnVector, ColumnarBatch}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import sttp.client3._

import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import scala.collection.convert.ImplicitConversions.`iterator asScala`

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

  @Param(Array("0", "1"))
  var compressionEnabled: String = _

  def generateSql(): String = s"""SELECT
                                |    randomPrintableASCII(10),
                                |    toInt32(rand())
                                |FROM numbers($rowNumber)
                                |FORMAT ArrowStream""".stripMargin

  @Benchmark
  def fetchSync(bh: Blackhole): Unit = {
    val sql = generateSql()
    val response = basicRequest
      .post(
        uri"http://127.0.0.1:8123/?enable_http_compression=$compressionEnabled"
      )
      .auth
      .basic("default", "")
      .acceptEncoding("gzip, deflate")
      .contentType("text/plain")
      .response(asByteArray)
      .body(sql)
      .send(backend)

    val bytes = response.body match {
      case Left(x) =>
        throw new Exception(x)
      case Right(x) =>
        x
    }

    val allocator = new RootAllocator(Long.MaxValue)
    val reader =
      new ArrowStreamReader(new ByteArrayInputStream(bytes), allocator)
    val root = reader.getVectorSchemaRoot

    while (reader.loadNextBatch()) {
      val arrowVectorIterator = root.getFieldVectors.iterator()
      val sparkVectors =
        arrowVectorIterator
          .map[ColumnVector] { arrowVector =>
            new ArrowColumnVector(arrowVector)
          }
          .toArray

      bh.consume(new ColumnarBatch(sparkVectors, root.getRowCount))
    }
  }
}
