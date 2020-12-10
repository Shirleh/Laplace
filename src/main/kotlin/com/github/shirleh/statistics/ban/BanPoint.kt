package com.github.shirleh.statistics.ban

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import java.time.Instant

data class BanPoint(
    val guildId: Snowflake,
    val author: Snowflake,
    val isBot: Boolean,
    val count: Long = 1L
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("ban")
        .addTag("guildId", guildId.asString())
        .addTag("author", author.asString())
        .addTag("isBot", isBot.toString())
        .addField("count", 1)
        .time(Instant.now(), WritePrecision.MS)
}
