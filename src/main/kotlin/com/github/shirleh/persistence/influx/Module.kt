package com.github.shirleh.persistence.influx

import com.influxdb.client.InfluxDBClient
import org.koin.dsl.module

val influxModule = module {
    single { InfluxConfiguration(get()) }
    single { InfluxClientFactory.create(get()) }
    single { get<InfluxDBClient>().writeApi }
}
