package com.github.shirleh.statistics.join

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

data class JoinPoint(
    val guildId: String,
    val creationDate: Instant,
    val isBoosting: Boolean,
    val joinTime: Instant,
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("member_join")
        .addTag("guildId", guildId)
        .addField("creationDate", creationDate.epochSecond)
        .addField("isBoosting", isBoosting)
        .time(joinTime, WritePrecision.MS)
}
