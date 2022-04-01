# Benchmarks for integrating with clickhouse

I'm about to build a datasource for integrating between clickhouse and spark which focus on speed.

But which way is the best?

## Thinking

When pull data from clickhouse to spark, the process can be divided into two parts:

1. pulling data which is related to data format, data type, rows and network.
2. converting data which is related to data format, data type and rows.

And at this time(2022-03-30), There are three ways for integrating:

1. official clickhouse-jdbc
   1. jdbc (too slow, ignore)
   2. http client (only support row based format right now)
   3. gRPC client (not stable yet, ignore)
   4. tcp client (not release yet, ignore)
2. ClickHouse-Native-JDBC
3. clickhouse-scala-client (not stable yet, ignore)
4. build from scratch with HTTP (I choose sttp library)

When using HTTP, there are so many data format to choose:

1. ArrowStream(can use in streaming mode)
2. Arrow
3. Parquet(common choice in big data)
4. Native(the most efficient choice as ClickHouse doc said, but not a good choice for converting)
5. JSONCompactEachRow(good choice for streaming mode)
6. RowBinary(good choice that combine the advantages of streaming ability and efficiency)

So this project are arisen, meant to find out the **fastest choice**...

## Usage

```bash
sbt -J-Xmx4G 'Jmh / run --jvmArgs "-Xms8G -Xmx32G -XX:MaxDirectMemorySize=16G" benchmarks.fetching.*'

sbt -J-Xmx4G 'Jmh / run --jvmArgs "-Xms8G -Xmx32G -XX:MaxDirectMemorySize=16G" benchmarks.parsing.*'
```

## Result

Here is a result on benchmarks.fetching.

```
Benchmark                              (compressionEnabled)                (dataFormat)  (rowNumber)  Mode  Cnt       Score       Error  Units
HttpClientTest.fetchAndDecompressSync                   N/A  RowBinaryWithNamesAndTypes       100000  avgt    5   16506.131 ±  3298.901  us/op
HttpClientTest.fetchAndDecompressSync                   N/A  RowBinaryWithNamesAndTypes      1000000  avgt    5   63250.282 ± 22530.931  us/op
Sttp.fetchSync                                            0                       Arrow       100000  avgt    5   14057.865 ±  3473.420  us/op
Sttp.fetchSync                                            0                       Arrow      1000000  avgt    5  104413.647 ± 23071.413  us/op
Sttp.fetchSync                                            0                 ArrowStream       100000  avgt    5   13583.656 ±  1803.950  us/op
Sttp.fetchSync                                            0                 ArrowStream      1000000  avgt    5  105957.914 ± 18600.060  us/op
Sttp.fetchSync                                            0                     Parquet       100000  avgt    5   35976.466 ±  7200.232  us/op
Sttp.fetchSync                                            0                     Parquet      1000000  avgt    5  293110.739 ± 39491.067  us/op
Sttp.fetchSync                                            0                      Native       100000  avgt    5   10258.884 ±  1536.501  us/op
Sttp.fetchSync                                            0                      Native      1000000  avgt    5   71823.960 ±  9162.643  us/op
Sttp.fetchSync                                            0                   RowBinary       100000  avgt    5   12892.156 ±  1179.377  us/op
Sttp.fetchSync                                            0                   RowBinary      1000000  avgt    5   61927.871 ± 25698.677  us/op
Sttp.fetchSync                                            1                       Arrow       100000  avgt    5   59926.785 ±  9801.555  us/op
Sttp.fetchSync                                            1                       Arrow      1000000  avgt    5  502041.636 ± 34938.050  us/op
Sttp.fetchSync                                            1                 ArrowStream       100000  avgt    5   59410.523 ±  2250.143  us/op
Sttp.fetchSync                                            1                 ArrowStream      1000000  avgt    5  499917.441 ± 24121.473  us/op
Sttp.fetchSync                                            1                     Parquet       100000  avgt    5   86210.968 ±  5490.753  us/op
Sttp.fetchSync                                            1                     Parquet      1000000  avgt    5  744051.378 ± 54858.212  us/op
Sttp.fetchSync                                            1                      Native       100000  avgt    5   46100.390 ±  4775.974  us/op
Sttp.fetchSync                                            1                      Native      1000000  avgt    5  402541.862 ± 26406.375  us/op
Sttp.fetchSync                                            1                   RowBinary       100000  avgt    5   54532.569 ± 23107.302  us/op
Sttp.fetchSync                                            1                   RowBinary      1000000  avgt    5  419122.062 ± 30184.169  us/op
ThirdPartyJdbc.fetch                                    N/A                         N/A       100000  avgt    5   17581.523 ±  4090.022  us/op
ThirdPartyJdbc.fetch                                    N/A                         N/A      1000000  avgt    5  114911.614 ± 15904.048  us/op
```
