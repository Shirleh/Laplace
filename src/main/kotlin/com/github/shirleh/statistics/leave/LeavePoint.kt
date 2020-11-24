package com.github.shirleh.statistics.leave

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Duration
import java.time.Instant

data class LeavePoint(
    val guildId: String,
    val userId: String?,
    val membershipDuration: Duration,
    val isBoosting: Boolean,
    val leaveTime: Instant
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("member_leave")
        .addTag("guildId", guildId)
        .addTag("userId", userId ?: "")
        .addField("membershipDuration", membershipDuration.seconds)
        .addField("isBoosting", isBoosting)
        .time(leaveTime, WritePrecision.MS)
}
