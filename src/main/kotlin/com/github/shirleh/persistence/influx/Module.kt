package com.github.shirleh.persistence.influx

import com.influxdb.client.InfluxDBClient
import mu.KotlinLogging
import org.koin.dsl.module

private val logger = KotlinLogging.logger { }

val influxModule = module {
    single(createdAtStart = true) { InfluxConfiguration(get()).also { logger.info { it } } }
    single { InfluxClientFactory.createClient(get()) }
    single { get<InfluxDBClient>().writeApi }
    single { InfluxClientFactory.getKotlinClient(get()).getQueryKotlinApi() }
}
