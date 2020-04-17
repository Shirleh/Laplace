package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.DiscordClient
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import java.time.Instant
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

suspend fun addDataCollectionListeners(client: DiscordClient) = coroutineScope {
    val pointRepository = PointRepositoryImpl()

    client.eventDispatcher.on(MessageCreateEvent::class.java).asFlow()
        .onEach {
            logger.trace { "Collecting MessageCreateEvent... " }

            Point.measurement("cupcakes")
                .addTag("color", "pink")
                .addField("score", Random.nextInt())
                .time(Instant.now(), WritePrecision.S)
                .let { pointRepository.save(it) }
        }
        .launchIn(this)

    client.eventDispatcher.on(MemberJoinEvent::class.java).asFlow()
        .onEach { logger.trace { "Collecting MemberJoinEvent... " } }
        .launchIn(this)
}
