package benchmarks.pulling

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.crobox.clickhouse.ClickhouseClient
import com.typesafe.config.{Config, ConfigFactory}
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
class ScalaClient {

  implicit val system: ActorSystem = ActorSystem.create("ClientTest")

  def generateConfig(): Config = ConfigFactory
    .parseString(s"""
                   |crobox.clickhouse {
                   |  client {
                   |    connection {
                   |      type = "single-host"
                   |      host = "127.0.0.1"
                   |      port = 8123
                   |      cluster = "default_cluster"
                   |      health-check {
                   |        interval = 10 seconds
                   |        timeout = 1 second
                   |      }
                   |      scanning-interval = 60 seconds
                   |      fallback-to-config-host-during-initialization = false
                   |    }
                   |    retries = 1
                   |    host-retrieval-timeout = 6 second
                   |    buffer-size = 1024
                   |    maximum-frame-length = 1048576
                   |    settings {
                   |      authentication {
                   |        user = "default"
                   |        password = ""
                   |      }
                   |      # profile = "default"
                   |      http-compression = true
                   |      custom {
                   |      }
                   |    }
                   |  }
                   |  indexer {
                   |    batch-size = 10000
                   |    concurrent-requests = 1
                   |    flush-interval = 5 seconds
                   |  }
                   |}
                   |""".stripMargin)

  @Param(Array("100000", "5000000"))
  var rowNumber: Int = _

  @Param(Array("ArrowStream", "Parquet"))
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

  //Client doesn't support auto decompression in ByteArray, that was too horrible for my situation.
//  @Benchmark
  def fetchAndDecompressSync(bh: Blackhole): Unit = {
    val config = generateConfig()
    val sql = generateSql()

    val client = new ClickhouseClient(Some(config))
    client
      .sourceByteString(sql)
      .runWith(Sink.foreach(byteString => bh.consume(byteString)))

  }
}
