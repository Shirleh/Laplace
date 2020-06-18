package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
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
}
