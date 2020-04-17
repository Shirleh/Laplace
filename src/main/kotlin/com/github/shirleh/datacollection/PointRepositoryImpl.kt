package com.github.shirleh.datacollection

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.write.Point

class PointRepositoryImpl : PointRepository {

    private val influxDBClient = InfluxDBClientFactory.create()

    override fun save(point: Point) = influxDBClient.writeApi.use { it.writePoint(point) }
}
