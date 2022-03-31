package tests

import com.clickhouse.client._
import org.apache.log4j.BasicConfigurator
import org.scalatest.funsuite.AnyFunSuite

class HttpClientTest extends AnyFunSuite {

  test("http client connection test") {
    BasicConfigurator.configure()
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
      .query("SELECT version()")
      .execute()
      .get()

    println(
      response
        .firstRecord()
        .getValue(0)
        .asString()
    )
    client.close()
  }

  test("http client origin stream test") {
    BasicConfigurator.configure()
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
    val response = request
      .compressServerResponse(true)
      .format(ClickHouseFormat.RowBinary)
      .query("""
               |SELECT
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
               |FROM numbers(100)
               |FORMAT RowBinary""".stripMargin)
      .execute()
      .get()

    response.pipe(System.out, 0)

    client.close()
  }

}
