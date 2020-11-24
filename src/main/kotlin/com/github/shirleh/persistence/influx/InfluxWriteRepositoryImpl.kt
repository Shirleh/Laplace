package com.github.shirleh.persistence.influx

import com.influxdb.client.WriteApi
import mu.KotlinLogging

abstract class InfluxWriteRepositoryImpl<T : InfluxPoint>(private val writeApi: WriteApi) : InfluxWriteRepository<T> {

    private val logger = KotlinLogging.logger { }

    override fun save(data: T) {
        logger.entry(data)

        val dataPoint = data.toDataPoint()
        writeApi.writePoint(dataPoint)

        logger.debug { "Saved $data" }
        logger.exit()
    }

    override fun save(data: List<T>) {
        logger.entry(data)

        val dataPoints = data.map(InfluxPoint::toDataPoint)
        writeApi.writePoints(dataPoints)

        logger.debug { "Saved $data" }
        logger.exit()
    }
}
