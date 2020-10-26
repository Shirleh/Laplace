package com.github.shirleh.statistics

import com.github.shirleh.persistence.influx.DataPointRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class VoiceStateData(
    val guildId: String,
    val channelId: String,
    val userId: String?,
    val isInVoice: Boolean,
    val isMuted: Boolean,
    val isSelfMuted: Boolean,
    val isDeafened: Boolean,
    val isSelfDeafened: Boolean,
) {
    fun toDataPoint() = Point.measurement("voiceState")
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

object VoiceDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    /**
     * Collects voice data from the incoming [events].
     */
    fun addListener(events: Flow<VoiceStateUpdateEvent>) = events
        .buffer()
        .map(VoiceDataCollector::toVoiceStateData)
        .onEach { logger.debug { it } }
        .map(VoiceStateData::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

    private suspend fun toVoiceStateData(event: VoiceStateUpdateEvent): VoiceStateData {
        logger.entry(event)

        val voiceState = event.current

        val guildId = voiceState.guildId
        val channelId = voiceState.channelId
            .map(Snowflake::asString)
            .orElseGet {
                event.old
                    .flatMap { oldVoiceState -> oldVoiceState.channelId.map(Snowflake::asString) }
                    .orElse("")
            }
        val userId = voiceState.userId

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(userId.asLong(), guildId.asLong())

        val result = VoiceStateData(
            guildId = guildId.asString(),
            channelId = channelId,
            userId = if (privacySettings?.voice == true) userId.asString() else null,
            isInVoice = voiceState.channelId.isPresent,
            isMuted = voiceState.isMuted,
            isSelfMuted = voiceState.isSelfMuted,
            isDeafened = voiceState.isDeaf,
            isSelfDeafened = voiceState.isSelfDeaf
        )

        return logger.exit(result)
    }
}
