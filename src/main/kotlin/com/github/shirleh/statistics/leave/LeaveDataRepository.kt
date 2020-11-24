package com.github.shirleh.statistics.leave

import com.github.shirleh.persistence.influx.InfluxWriteRepository
import com.github.shirleh.persistence.influx.InfluxWriteRepositoryImpl
import com.influxdb.client.WriteApi

internal interface LeavePointRepository : InfluxWriteRepository<LeavePoint>

internal class LeavePointRepositoryImpl(writeApi: WriteApi) : LeavePointRepository,
    InfluxWriteRepositoryImpl<LeavePoint>(writeApi)
