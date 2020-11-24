package com.github.shirleh.statistics.voice

import com.github.shirleh.persistence.influx.InfluxPoint
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

data class VoicePoint(
    val guildId: String,
    val channelId: String,
    val userId: String?,
    val isInVoice: Boolean,
    val isMuted: Boolean,
    val isSelfMuted: Boolean,
    val isDeafened: Boolean,
    val isSelfDeafened: Boolean,
) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("voiceState")
        .addTag("guildId", guildId)
        .addTag("channelId", channelId)
        .addTag("userId", userId ?: "")
        .addField("isInVoice", isInVoice)
        .addField("isMuted", isMuted)
        .addField("isSelfMuted", isSelfMuted)
        .addField("isDeafened", isDeafened)
        .addField("isSelfDeafened", isSelfDeafened)
        .time(Instant.now(), WritePrecision.MS)
}
