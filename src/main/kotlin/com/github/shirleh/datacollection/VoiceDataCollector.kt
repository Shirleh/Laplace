package com.github.shirleh.datacollection

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

object VoiceDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects voice data from the incoming [events].
     */
    suspend fun collect(events: Flow<VoiceStateUpdateEvent>) {
        events
            .map(::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toDataPoint(event: VoiceStateUpdateEvent): Point {
        logger.entry(event)

        val voiceState = event.current

        val guildId = voiceState.guildId.asString()
        val guildMemberId = voiceState.userId.asString()
        val channelId = voiceState.channelId
            .map(Snowflake::asString)
            .orElseGet {
                event.old
                    .flatMap { old -> old.channelId.map { it.asString() } }
                    .orElse("")
            }

        val isMuted = voiceState.isMuted || voiceState.isSelfMuted
        val isDeafened = voiceState.isDeaf || voiceState.isSelfDeaf
        val isInVoice = voiceState.channelId.isPresent

        val result = Point.measurement("voiceState")
            .addTag("guildId", guildId)
            .addTag("channelId", channelId)
            .addTag("guildMemberId", guildMemberId)
            .addField("isMuted", isMuted)
            .addField("isDeafened", isDeafened)
            .addField("isInVoice", isInVoice)
            .time(Instant.now(), WritePrecision.S)

        return logger.exit(result)
    }
}
