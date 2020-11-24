package com.github.shirleh.statistics.nickname

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface NicknamePointRepository : InfluxWriteRepository<NicknamePoint>

internal class NicknamePointRepositoryImpl(writeApi: WriteApi) : NicknamePointRepository,
    InfluxWriteRepositoryImpl<NicknamePoint>(writeApi)
