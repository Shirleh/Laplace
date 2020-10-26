package com.github.shirleh.statistics.emoji

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

internal enum class Source { MESSAGE, REACTION }
internal enum class Type { UNICODE, CUSTOM }

internal data class Emoji(
    val guildId: String,
    val userId: String?,
    val source: Source,
    val type: Type,
    val id: String,
    val count: Long = 1L,
) {
    fun toDataPoint(): Point = Point.measurement("emoji")
        .addTag("guildId", guildId)
        .addTag("userId", userId ?: "")
        .addTag("source", source.name)
        .addTag("type", type.name)
        .addField(id, count)
        .time(Instant.now(), WritePrecision.MS)
}
