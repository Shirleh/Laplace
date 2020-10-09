package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import mu.KotlinLogging
import java.time.Instant

object DataCollectionHandler {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository = DataPointRepositoryImpl()

    /**
     * Collects voice data from the incoming [events].
     */
    suspend fun collectVoiceData(events: Flow<VoiceStateUpdateEvent>) = events.collect { saveVoiceData(it) }

    private fun saveVoiceData(event: VoiceStateUpdateEvent) {
        logger.entry(event)

        val voiceState = event.current

        val guildId = voiceState.guildId.asString()
        val guildMemberId = voiceState.userId.asString()

        val channelId = voiceState.channelId
            .map { it.asString() }
            .orElseGet {
                event.old
                    .flatMap { old -> old.channelId.map { it.asString() } }
                    .orElse("")
            }

        val isMuted = voiceState.isMuted || voiceState.isSelfMuted
        val isDeafened = voiceState.isDeaf || voiceState.isSelfDeaf
        val isInVoice = voiceState.channelId.isPresent

        Point.measurement("voiceState")
            .addTag("guildId", guildId)
            .addTag("channelId", channelId)
            .addTag("guildMemberId", guildMemberId)
            .addField("isMuted", isMuted)
            .addField("isDeafened", isDeafened)
            .addField("isInVoice", isInVoice)
            .time(Instant.now(), WritePrecision.S)
            .let { dataPointRepository.save(it) }

        logger.exit()
    }
}
