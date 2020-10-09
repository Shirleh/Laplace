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

/**
 * Arbitrary delay to prevent audit log access before it's updated.
 *
 * In Discord, there's a delay between dispatching an event and updating the audit log with said event.
 * Larger guilds tend to have longer delays. 5 seconds is commonly used which works *most of the time*.
 */
private const val AUDIT_LOG_UPDATE_DELAY = 5000L

object DataCollectionHandler {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository = DataPointRepositoryImpl()

    /**
     * Collects member join data from the incoming [events].
     */
    suspend fun collectJoinData(events: Flow<MemberJoinEvent>) = events.collect { saveJoinData(it) }

    private fun saveJoinData(event: MemberJoinEvent) {
        logger.entry(event)

        val member = event.member

        val guildId = event.guildId.asString()
        val guildMemberId = member.id.asString()
        val creationDate = member.id.timestamp.epochSecond
        val isBot = member.isBot
        val timestamp = member.joinTime

        Point.measurement("guildMembership")
            .addTag("event", "join")
            .addTag("guildId", guildId)
            .addTag("guildMemberId", guildMemberId)
            .addField("creationDate", creationDate)
            .addField("isBot", isBot)
            .time(timestamp, WritePrecision.S)
            .let { dataPointRepository.save(it) }

        logger.exit()
    }

    /**
     * Collects member leave data from the incoming [events].
     */
    suspend fun collectLeaveData(events: Flow<MemberLeaveEvent>) = events.collect { saveLeaveData(it) }

    private suspend fun saveLeaveData(event: MemberLeaveEvent) {
        logger.entry(event)

        val guildMemberId = event.user.id.asString()
        val guildId = event.guildId.asString()
        val joinTime = event.member.map { it.joinTime }
            .orElseThrow { IllegalStateException("Member not present in MemberLeaveEvent?") }
        val leaveTime = Instant.now()
        val membershipDuration = Duration.between(joinTime, leaveTime)

        Point.measurement("guildMembership")
            .addTag("event", "leave")
            .addTag("guildId", guildId)
            .addTag("guildMemberId", guildMemberId)
            .addField("membershipDuration", membershipDuration.seconds)
            .time(leaveTime, WritePrecision.S)
            .let { dataPointRepository.save(it) }

        logger.exit()
    }

    /**
     * Collects ban data from the incoming [events].
     */
    suspend fun collectBanData(events: Flow<BanEvent>) =
        events
            .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
            .mapNotNull(::findBanAuthorId)
            .collect(::saveBanData)

    private suspend fun findBanAuthorId(event: BanEvent): Snowflake? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD) }
            .asFlow()
            .filter { auditLogEntry -> auditLogEntry.targetId.orElse(null) == event.user.id }
            .map { auditLogEntry -> auditLogEntry.responsibleUserId }
            .firstOrNull()

        return result.also { logger.exit(it) }
    }

    private suspend fun saveBanData(banAuthorId: Snowflake) {
        logger.entry(banAuthorId)

        Point.measurement("bans")
            .addTag("author", banAuthorId.asString())
            .addField("count", 1)
            .time(Instant.now(), WritePrecision.S)
            .run(dataPointRepository::save)

        logger.exit()
    }

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
