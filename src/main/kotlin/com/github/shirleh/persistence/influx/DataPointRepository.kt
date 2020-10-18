package com.github.shirleh.persistence.influx

import com.influxdb.client.write.Point

/**
 * A repository used to write data points into InfluxDB.
 */
interface DataPointRepository {

    /**
     * Saves a data [point] into InfluxDB.
     */
    fun save(point: Point)

    /**
     * Saves all data [points] into InfluxDB.
     */
    fun save(points: List<Point>)
}
