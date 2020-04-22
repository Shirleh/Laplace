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

suspend fun addDataCollectionListeners(client: DiscordClient) = coroutineScope {
    val pointRepository = PointRepositoryImpl()

    client.eventDispatcher.on(MessageCreateEvent::class.java).asFlow()
        .onEach { event ->
            logger.trace { "Collecting data from MessageCreateEvent..." }

            val channelId = event.message.channelId.asString()
            val authorId = event.message.author.map { it.id.asString() }.orElse(null) ?: return@onEach
            val content = event.message.content.orElse(null) ?: return@onEach
            val timestamp = event.message.timestamp

            Point.measurement("message")
                .addTag("channel", channelId)
                .addTag("author", authorId)
                .addField("length", content.length)
                .time(timestamp, WritePrecision.S)
                .let { pointRepository.save(it) }

            logger.trace { "Collected data from MessageCreateEvent" }
        }
        .launchIn(this)

    client.eventDispatcher.on(MemberJoinEvent::class.java).asFlow()
        .onEach { logger.trace { "Collecting MemberJoinEvent... " } }
        .launchIn(this)
}
