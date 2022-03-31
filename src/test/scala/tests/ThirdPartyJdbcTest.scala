package tests

import org.scalatest.funsuite.AnyFunSuite

import java.sql.{Connection, DriverManager, ResultSet, Statement}

class ThirdPartyJdbcTest extends AnyFunSuite {
  test("third party jdbc connection test") {
    var connection: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    try {
      connection =
        DriverManager.getConnection("jdbc:clickhouse://127.0.0.1:9000")
      stmt = connection.createStatement
      rs = stmt.executeQuery("SELECT version()")
      rs.next()
      println(s"Connection succeed! ClickHouse version is '${rs.getString(1)}'")
    } finally {
      if (rs != null) {
        rs.close()
      }
      if (stmt != null) {
        stmt.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
  }
}
