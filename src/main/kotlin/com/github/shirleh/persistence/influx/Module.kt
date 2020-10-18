package com.github.shirleh.persistence.influx

import com.influxdb.client.InfluxDBClient
import org.koin.dsl.module

val influxModule = module {
    single(createdAtStart = true) { InfluxConfiguration(get()) }
    single { InfluxClientFactory.create(get()) }
    single { get<InfluxDBClient>().writeApi }

    single<DataPointRepository> { DataPointRepositoryImpl(get()) }
}
