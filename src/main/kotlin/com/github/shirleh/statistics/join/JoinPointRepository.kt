package com.github.shirleh.statistics.join

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface JoinPointRepository : InfluxWriteRepository<JoinPoint>

internal class JoinPointRepositoryImpl(writeApi: WriteApi) : JoinPointRepository,
    InfluxWriteRepositoryImpl<JoinPoint>(writeApi)
