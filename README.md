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
sbt -J-Xmx4G 'Jmh / run --jvmArgs "-Xms8G -Xmx32G -XX:MaxDirectMemorySize=16G" benchmark*'
```
