package com.github.shirleh.statistics.emoji

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface EmojiPointRepository : InfluxWriteRepository<EmojiPoint>

internal class EmojiPointRepositoryImpl(writeApi: WriteApi) : EmojiPointRepository,
    InfluxWriteRepositoryImpl<EmojiPoint>(writeApi)
