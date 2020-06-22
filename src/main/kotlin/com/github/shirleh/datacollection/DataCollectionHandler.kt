package com.github.shirleh.datacollection

import com.github.shirleh.orElseNull
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.`object`.audit.ActionType
import discord4j.core.`object`.audit.AuditLogEntry
import discord4j.core.`object`.audit.ChangeKey
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
    private val guildMemberQueryRepository = GuildMemberQueryRepositoryImpl()

    /**
     * Collects message data from the incoming [flow] of [MessageCreateEvent]s.
     */
    suspend fun collectMessageData(flow: Flow<MessageCreateEvent>) = flow
        .collect { event ->
            logger.entry(event)

            val message = event.message

            val channelId = message.channelId.asString()
            val authorId = message.author.map { it.id.asString() }.orElse(null) ?: return@collect
            val contentLength = message.content.let { if (it.isBlank()) return@collect else it.length }
            val timestamp = message.timestamp

            Point.measurement("message")
                .addTag("channel", channelId)
                .addTag("author", authorId)
                .addField("length", contentLength)
                .time(timestamp, WritePrecision.S)
                .let { dataPointRepository.save(it) }

            logger.exit()
        }

    /**
     * Collects member join data from the incoming [flow] of [MemberJoinEvent]s.
     */
    suspend fun collectJoinData(flow: Flow<MemberJoinEvent>) = flow
        .collect { event ->
            logger.entry(event)

            val member = event.member

            val guildId = event.guildId.asString()
            val guildMemberId = member.id.asString()
            val creationDate = member.id.timestamp.epochSecond
            val isBot = member.isBot
            val timestamp = member.joinTime

            Point.measurement("guildMember")
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
     * Collects member leave data from the incoming [flow] of [MemberLeaveEvent]s.
     */
    suspend fun collectLeaveData(flow: Flow<MemberLeaveEvent>) = flow
        .collect { event ->
            logger.entry(event)

            val guildMemberId = event.user.id.asString()
            val guildId = event.guildId.asString()
            val joinTime = guildMemberQueryRepository.findLatestJoinDate(guildMemberId, guildId)
            val leaveTime = Instant.now()
            val membershipDuration = Duration.between(joinTime, leaveTime)

            Point.measurement("guildMember")
                .addTag("event", "leave")
                .addTag("guildId", guildId)
                .addTag("guildMemberId", guildMemberId)
                .addField("membershipDuration", membershipDuration.seconds)
                .time(leaveTime, WritePrecision.S)
                .let { dataPointRepository.save(it) }

            logger.exit()
        }

    /**
     * Collects member nickname data from the incoming [events].
     */
    suspend fun collectNicknameData(events: Flow<MemberUpdateEvent>) {
        logger.entry()

        events
            .onEach { delay(5000) }
            .collect { event ->
                val auditLog = findAuditLogByEvent(event) ?: return@collect
                saveNicknameData(auditLog)
            }

        logger.exit()
    }

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
}
