package com.github.shirleh.statistics.voice

import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object VoiceDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val voiceDataRepository: VoicePointRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    /**
     * Collects voice data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
    fun addListener(events: Flow<VoiceStateUpdateEvent>) = events
        .buffer()
        .flatMapConcat { event ->
            flowOf(event)
                .map(VoiceDataCollector::aggregateVoiceData)
                .onEach(voiceDataRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private suspend fun aggregateVoiceData(event: VoiceStateUpdateEvent): VoicePoint {
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

        val result = VoicePoint(
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
