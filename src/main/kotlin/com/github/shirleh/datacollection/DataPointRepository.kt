package com.github.shirleh.datacollection

import com.influxdb.client.write.Point

/**
 * A repository used to write data points into InfluxDB.
 */
interface DataPointRepository {

    /**
     * Saves a data [point] into InfluxDB.
     */
    fun save(point: Point)
}
