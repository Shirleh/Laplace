package com.github.shirleh.persistence.influx

import com.influxdb.LogLevel
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import java.io.File

private data class InfluxProperties(
    val url: String,
    @Suppress("ArrayInDataClass") val token: CharArray,
    val defaultOrg: String,
    val defaultBucket: String
)

object InfluxClientFactory {

    fun getKotlinClient(config: InfluxConfiguration): InfluxDBClientKotlin {
        val (url, token, org, bucket) = readInfluxProperties(config)
        return InfluxDBClientKotlinFactory.create(url, token, org, bucket)

    }

    fun createClient(config: InfluxConfiguration): InfluxDBClient {
        val (url, token, org, bucket) = readInfluxProperties(config)
        return InfluxDBClientFactory.create(url, token, org, bucket).apply {
            logLevel = LogLevel.valueOf(config.influxLogLevel)
        }
    }

    private fun readInfluxProperties(config: InfluxConfiguration): InfluxProperties {
        val file = File(System.getenv("INFLUX2_TOKEN_FILE") ?: "influx2_token.txt")
        val token = file.readText().trim().toCharArray()

        return InfluxProperties(
            url = config.influxUrl,
            token = token,
            defaultOrg = config.influxDefaultOrg,
            defaultBucket = config.influxDefaultBucket,
        )
    }
}
