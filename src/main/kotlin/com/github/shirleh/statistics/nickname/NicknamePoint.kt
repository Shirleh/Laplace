package com.github.shirleh.statistics.nickname

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

data class NicknamePoint(
    val userId: String?,
    val newNickname: String,
    val hadNickname: Boolean,
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("nickname")
        .addTag("userId", userId ?: "")
        .addField("nickname", newNickname)
        .addField("hadNickname", hadNickname)
        .time(Instant.now(), WritePrecision.MS)
}
