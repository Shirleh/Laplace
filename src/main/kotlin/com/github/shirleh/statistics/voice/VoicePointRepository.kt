package com.github.shirleh.statistics.voice

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface VoicePointRepository : InfluxWriteRepository<VoicePoint>

internal class VoicePointRepositoryImpl(writeApi: WriteApi) : VoicePointRepository,
    InfluxWriteRepositoryImpl<VoicePoint>(writeApi)
