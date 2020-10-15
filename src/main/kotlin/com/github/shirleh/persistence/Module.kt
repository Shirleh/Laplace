package com.github.shirleh.persistence

import com.influxdb.client.InfluxDBClient
import org.koin.dsl.module

val persistenceModule = module {
    single { InfluxClientFactory.create(get()) }
    single { get<InfluxDBClient>().writeApi }
}
