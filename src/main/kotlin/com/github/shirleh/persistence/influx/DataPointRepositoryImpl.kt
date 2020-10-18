package com.github.shirleh.persistence.influx

import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point

class DataPointRepositoryImpl(private val writeApi: WriteApi) : DataPointRepository {

    override fun save(point: Point) = writeApi.writePoint(point)

    override fun save(points: List<Point>) = writeApi.writePoints(points)
}
