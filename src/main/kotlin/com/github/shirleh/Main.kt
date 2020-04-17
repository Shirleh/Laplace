package com.github.shirleh

import com.github.shirleh.command.CommandParser
import com.github.shirleh.command.CommandRegistry
import com.github.shirleh.datacollection.PointRepositoryImpl
import com.github.shirleh.monitoring.MonitoringCommandSet
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Instant
import kotlin.random.Random
import kotlin.system.exitProcess

val logger = KotlinLogging.logger { }

fun main(args: Array<String>) = runBlocking<Unit> {
    if (args.isEmpty()) {
        System.err.println("Please provide a Discord token for the bot.")
        exitProcess(1)
    }
    val token = args[0]
    val client = DiscordClientBuilder(token).build()

    val commandRepository = CommandRegistry
        .register(MonitoringCommandSet)

    client.eventDispatcher.on(MessageCreateEvent::class.java).asFlow()
        .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
        .onEach { event ->
            logger.trace { event.message.content.orElse("") }

            val parser = event.message.content.map { CommandParser(it) }.orElse(null) ?: return@onEach

            val selfId = client.selfId.orElse(null) ?: return@onEach
            val mention = parser.next()
            if (mention != """<@!${selfId.asString()}>""") return@onEach

            val commandName = parser.next() ?: return@onEach
            val command = commandRepository.findByName(commandName) ?: return@onEach
            command.handler.invoke(parser, event)
        }
        .launchIn(this)

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

    client.login().awaitFirstOrNull()
}
