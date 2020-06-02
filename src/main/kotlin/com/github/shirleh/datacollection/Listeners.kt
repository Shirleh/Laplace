package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.DiscordClient
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun addDataCollectionListeners(client: DiscordClient) {
    val dataPointRepository = DataPointRepositoryImpl()

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
}
