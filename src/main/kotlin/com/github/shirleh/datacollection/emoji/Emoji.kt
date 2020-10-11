package com.github.shirleh.datacollection.emoji

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant

internal enum class Source { MESSAGE, REACTION }
internal enum class Type { UNICODE, CUSTOM }

internal data class Emoji(
    val guildId: String,
    val channelId: String,
    val source: Source,
    val type: Type,
    val value: String
) {
    @OptIn(ExperimentalStdlibApi::class)
    fun toDataPoint(): Point =
        Point.measurement("emoji")
            .addTag("guildId", guildId)
            .addTag("channeId", channelId)
            .addTag("source", source.name)
            .addTag("type", type.name)
            .addField("id", value)
            .time(Instant.now(), WritePrecision.S)
}
