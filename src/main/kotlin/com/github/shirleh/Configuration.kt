package com.github.shirleh

import com.typesafe.config.Config

class Configuration(config: Config) {
    val superuserRoleId: Long = config.getLong("discord.superuserRoleId")

    val influxUrl: String = config.getString("influx2.url")
    val influxDefaultOrg: String = config.getString("influx2.org")
    val influxDefaultBucket: String = config.getString("influx2.bucket")
}
