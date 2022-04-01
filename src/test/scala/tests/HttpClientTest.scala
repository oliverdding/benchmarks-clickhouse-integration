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
      .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
      .query("""
               |SELECT
               |    randomPrintableASCII(10),
               |    toInt32(rand())
               |FROM numbers(100)
               |FORMAT RowBinaryWithNamesAndTypes""".stripMargin)
      .execute()
      .get()

    var cnt = 0
    response
      .records()
      .forEach(r =>
        r.forEach(v => {
          cnt += 1
          println(v.asString())
        })
      )
    println(cnt)

    client.close()
  }

}
