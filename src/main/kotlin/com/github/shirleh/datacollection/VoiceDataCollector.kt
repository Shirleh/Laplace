package com.github.shirleh.datacollection

import com.github.shirleh.persistence.influx.DataPointRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class VoiceStateData(
    val guildId: String,
    val channelId: String,
    val memberId: String,
    val isInVoice: Boolean,
    val isMuted: Boolean,
    val isSelfMuted: Boolean,
    val isDeafened: Boolean,
    val isSelfDeafened: Boolean,
) {
    fun toDataPoint() = Point.measurement("voiceState")
        .addTag("guildId", guildId)
        .addTag("channelId", channelId)
        .addTag("guildMemberId", memberId)
        .addField("isInVoice", isInVoice)
        .addField("isMuted", isMuted)
        .addField("isSelfMuted", isSelfMuted)
        .addField("isDeafened", isDeafened)
        .addField("isSelfDeafened", isSelfDeafened)
        .time(Instant.now(), WritePrecision.S)
}

object VoiceDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects voice data from the incoming [events].
     */
    suspend fun collect(events: Flow<VoiceStateUpdateEvent>) {
        events
            .map(VoiceDataCollector::toVoiceStateData)
            .map(VoiceStateData::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toVoiceStateData(event: VoiceStateUpdateEvent): VoiceStateData {
        logger.entry(event)

        val voiceState = event.current
        val guildId = voiceState.guildId.asString()
        val memberId = voiceState.userId.asString()
        val channelId = voiceState.channelId
            .map(Snowflake::asString)
            .orElseGet {
                event.old
                    .flatMap { old -> old.channelId.map { it.asString() } }
                    .orElse("")
            }

        val result = VoiceStateData(
            guildId = guildId,
            channelId = channelId,
            memberId = memberId,
            isInVoice = voiceState.channelId.isPresent,
            isMuted = voiceState.isMuted,
            isSelfMuted = voiceState.isSelfMuted,
            isDeafened = voiceState.isDeaf,
            isSelfDeafened = voiceState.isSelfDeaf
        )

        return logger.exit(result)
    }
}
