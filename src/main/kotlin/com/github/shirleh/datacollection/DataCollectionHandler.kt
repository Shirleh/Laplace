package com.github.shirleh.datacollection

import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.`object`.audit.ActionType
import discord4j.core.`object`.audit.AuditLogEntry
import discord4j.core.`object`.audit.ChangeKey
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.BanEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

object DataCollectionHandler {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository = DataPointRepositoryImpl()

    /**
     * Collects member nickname data from the incoming [events].
     */
    suspend fun collectNicknameData(events: Flow<MemberUpdateEvent>) =
        events
            .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
            .mapNotNull { findAuditLogByEvent(it) }
            .collect { saveNicknameData(it) }

    private suspend fun findAuditLogByEvent(event: MemberUpdateEvent): AuditLogEntry? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_UPDATE) }
            .asFlow()
            .filter { auditLogEntry ->
                auditLogEntry.getChange(ChangeKey.USER_NICK)
                    .map { it.currentValue == event.currentNickname }
                    .orElse(false)
            }
            .firstOrNull()

        return result.also { logger.exit(it) }
    }

    private fun saveNicknameData(auditLog: AuditLogEntry) {
        logger.entry(auditLog)

        val author = auditLog.responsibleUserId.asString()

        val change = auditLog.getChange(ChangeKey.USER_NICK).orElseNull() ?: return
        val oldNick = change.oldValue.orElse("")
        val currentNick = change.currentValue.orElse("")

        Point.measurement("nicknames")
            .addTag("author", author)
            .addField("oldNick", oldNick)
            .addField("currentNick", currentNick)
            .time(Instant.now(), WritePrecision.S)
            .let { dataPointRepository.save(it) }

        logger.exit()
    }

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
