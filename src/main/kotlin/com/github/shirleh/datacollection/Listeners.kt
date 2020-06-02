package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.DiscordClient
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

fun addDataCollectionListeners(client: DiscordClient) {
    val dataPointRepository = DataPointRepositoryImpl()
    val guildMemberQueryRepository = GuildMemberQueryRepositoryImpl()

    client.eventDispatcher.on(MessageCreateEvent::class.java).asFlow()
        .onEach { event ->
            logger.entry(event)

            val channelId = event.message.channelId.asString()
            val authorId = event.message.author.map { it.id.asString() }.orElse(null) ?: return@onEach
            val content = event.message.content.orElse(null) ?: return@onEach
            val timestamp = event.message.timestamp

            Point.measurement("message")
                .addTag("channel", channelId)
                .addTag("author", authorId)
                .addField("length", content.length)
                .time(timestamp, WritePrecision.S)
                .let { dataPointRepository.save(it) }

            logger.exit()
        }
        .launchIn(GlobalScope)

    client.eventDispatcher.on(MemberJoinEvent::class.java).asFlow()
        .onEach {event ->
            logger.entry(event)

            val guildId = event.guildId.asString()
            val guildMemberId = event.member.id.asString()
            val creationDate = event.member.id.timestamp.epochSecond
            val isBot = event.member.isBot
            val timestamp = event.member.joinTime

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
        .launchIn(GlobalScope)

    client.eventDispatcher.on(MemberLeaveEvent::class.java).asFlow()
        .onEach { event ->
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
        .launchIn(GlobalScope)
}
