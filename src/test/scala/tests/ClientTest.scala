package tests

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.crobox.clickhouse.ClickhouseClient
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class ClientTest extends AnyFunSuite {

  implicit val system: ActorSystem = ActorSystem.create("ClientTest")

  lazy val config: Config = ConfigFactory
    .parseString("""
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
                   |      profile = "default"
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

  test("client connection test") {
    val client = new ClickhouseClient(Some(config))
    val future = client
      .query("SELECT version()")
      .map(ver => println(ver))
    Await.ready(future, 3 seconds)
  }

  test("client compression test sync") {
    val client = new ClickhouseClient(Some(config))
    val future = client
      .query(
        """
          |SELECT
          |    randomPrintableASCII(128),
          |    toInt32(rand())
          |FROM numbers(3)
          |FORMAT JSON
          |""".stripMargin
      ).map(ver => println(ver))
    Await.ready(future, 3 seconds)
  }

  test("client compression test async") {
    val client = new ClickhouseClient(Some(config))
    val future = client
      .query(
        """
          |SELECT
          |    randomPrintableASCII(128),
          |    toInt32(rand())
          |FORMAT JSON
          |""".stripMargin
      ).map(ver => println(ver))
    Await.ready(future, 3 seconds)
  }
}
