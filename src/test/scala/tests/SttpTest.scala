package tests

import org.scalatest.funsuite.AnyFunSuite
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend, UriContext, asString, basicRequest}

class SttpTest extends AnyFunSuite {
  test("sttp connection test") {
    lazy val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
    val response = basicRequest
      .post(uri"http://127.0.0.1:8123/")
      .auth
      .basic("default", "")
      .contentType("text/plain")
      .response(asString.getRight)
      .body("SELECT version()")
      .send(backend)

    println(response.body)
    backend.close()
  }

  test("sttp compression test") {
    lazy val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
    val response = basicRequest
      .post(uri"http://127.0.0.1:8123/?enable_http_compression=1")
      .auth
      .basic("default", "")
      .acceptEncoding("deflate")
      .contentType("text/plain")
      .response(asString.getRight)
      .body("SELECT version()")
      .send(backend)

    println(response.body)
    backend.close()
  }
}
