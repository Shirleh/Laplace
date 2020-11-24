package com.github.shirleh.statistics.message

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

data class MessagePoint(
    val guildId: String,
    val channelId: String,
    val userId: String?,
    val length: Int,
    val wordCount: Int,
    val timestamp: Instant
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("message")
        .addTag("guildId", guildId)
        .addTag("channel", channelId)
        .addTag("userId", userId ?: "")
        .addField("length", length)
        .addField("wordCount", wordCount)
        .time(timestamp, WritePrecision.MS)
}
