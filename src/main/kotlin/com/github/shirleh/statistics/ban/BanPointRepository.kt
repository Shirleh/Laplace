package com.github.shirleh.statistics.ban

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface BanPointRepository : InfluxWriteRepository<BanPoint>

internal class BanPointRepositoryImpl(writeApi: WriteApi) : BanPointRepository,
    InfluxWriteRepositoryImpl<BanPoint>(writeApi)
