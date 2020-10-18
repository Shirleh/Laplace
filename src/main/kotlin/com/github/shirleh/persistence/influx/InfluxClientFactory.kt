package com.github.shirleh.persistence.influx

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import mu.KotlinLogging
import java.io.File

private data class InfluxProperties(
    val url: String,
    @Suppress("ArrayInDataClass") val token: CharArray,
    val defaultOrg: String,
    val defaultBucket: String
)

object InfluxClientFactory {

    private val logger = KotlinLogging.logger { }
    
    fun create(config: InfluxConfiguration): InfluxDBClient {
        val (url, token, org, bucket) = readInfluxProperties(config)
        return InfluxDBClientFactory.create(url, token, org, bucket)
    }

    private fun readInfluxProperties(config: InfluxConfiguration): InfluxProperties {
        val file = File(System.getenv("INFLUX2_TOKEN_FILE") ?: "influx2_token.txt")
        val token = file.readText().trim().toCharArray()

        return InfluxProperties(
            url = config.influxUrl,
            token = token,
            defaultOrg = config.influxDefaultOrg,
            defaultBucket = config.influxDefaultBucket,
        ).also { logger.info { "Starting InfluxDB with $it" } }
    }
}
