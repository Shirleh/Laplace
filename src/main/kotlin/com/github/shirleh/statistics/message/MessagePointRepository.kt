package com.github.shirleh.statistics.message

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface MessagePointRepository : InfluxWriteRepository<MessagePoint>

internal class MessagePointRepositoryImpl(writeApi: WriteApi) : MessagePointRepository,
    InfluxWriteRepositoryImpl<MessagePoint>(writeApi)
