package com.github.shirleh.persistence.influx

import com.typesafe.config.Config

class InfluxConfiguration(config: Config) {
    val influxUrl: String = config.getString("influx2.url")
    val influxDefaultOrg: String = config.getString("influx2.org")
    val influxDefaultBucket: String = config.getString("influx2.bucket")
}
