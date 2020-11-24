package com.github.shirleh.persistence.influx

/**
 * A repository used to write data points into InfluxDB.
 */
interface InfluxWriteRepository<T : InfluxPoint> {

    /**
     * Saves [data] into InfluxDB.
     */
    fun save(data: T)

    /**
     * Saves [data] into InfluxDB.
     */
    fun save(data: List<T>)
}
