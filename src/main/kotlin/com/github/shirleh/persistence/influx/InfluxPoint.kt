package com.github.shirleh.persistence.influx

import com.influxdb.client.write.Point

interface InfluxPoint {
    fun toDataPoint(): Point
}
