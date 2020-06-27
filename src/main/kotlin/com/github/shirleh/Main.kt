package com.github.shirleh

import com.github.shirleh.command.CommandHandler
import com.github.shirleh.datacollection.DataCollectionHandler
import discord4j.core.DiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.util.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

fun main() = runBlocking<Unit> {
    val token = getToken()
    if (token == null) {
        logger.error { "Missing Discord bot token" }
        exitProcess(1)
    }

    DiscordClient.create(token).withGateway { client ->
        mono {
            launch { client.on(MessageCreateEvent::class.java).addListener(CommandHandler::executeCommands) }
            launch { client.on(MessageCreateEvent::class.java).addListener(DataCollectionHandler::collectMessageData) }
            launch { client.on(MemberJoinEvent::class.java).addListener(DataCollectionHandler::collectJoinData) }
            launch { client.on(MemberLeaveEvent::class.java).addListener(DataCollectionHandler::collectLeaveData) }
            launch { client.on(MemberUpdateEvent::class.java).addListener(DataCollectionHandler::collectNicknameData) }
            launch { client.on(VoiceStateUpdateEvent::class.java).addListener(DataCollectionHandler::collectVoiceData) }
        }
    }.block()
}

private fun getToken(): String? {
    logger.entry()

    val filePath = Path.of(System.getenv("DISCORD_TOKEN_FILE") ?: "discord_token.txt")
    val scanner = Scanner(filePath)
    val result = if (scanner.hasNext()) scanner.next() else null

    return result.also { logger.exit(it) }
}

private suspend fun <E : Event> Flux<E>.addListener(listener: suspend (Flow<E>) -> Unit) = listener.invoke(asFlow())
