package com.github.shirleh.datacollection

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.write.Point

class DataPointRepositoryImpl : DataPointRepository {

    private val influxDBClient = InfluxDBClientFactory.create()

    override fun save(point: Point) = influxDBClient.writeApi.use { it.writePoint(point) }

    override fun save(points: List<Point>) = influxDBClient.writeApi.use { it.writePoints(points) }
}
