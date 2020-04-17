package com.github.shirleh.datacollection

import com.influxdb.client.write.Point

/**
 * A repository used to save data points into InfluxDB.
 */
interface PointRepository {

    /**
     * Saves a data [point] into InfluxDB.
     */
    fun save(point: Point)
}
