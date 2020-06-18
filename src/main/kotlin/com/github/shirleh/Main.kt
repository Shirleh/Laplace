package com.github.shirleh

import com.github.shirleh.command.CommandHandler
import com.github.shirleh.datacollection.DataCollectionHandler
import discord4j.core.DiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking<Unit> {
    val token = getToken(args)
    if (token == null) {
        System.err.println("Please provide a Discord token for the bot.")
        exitProcess(1)
    }

    DiscordClient.create(token).withGateway { client ->
        mono {
            launch { client.on(MessageCreateEvent::class.java).addListener(CommandHandler::executeCommands) }
            launch { client.on(MessageCreateEvent::class.java).addListener(DataCollectionHandler::collectMessageData) }
            launch { client.on(MemberJoinEvent::class.java).addListener(DataCollectionHandler::collectJoinData) }
            launch { client.on(MemberLeaveEvent::class.java).addListener(DataCollectionHandler::collectLeaveData) }
        }
    }.block()
}

private fun getToken(args: Array<String>): String? =
    if (args.isNotEmpty()) args[0]
    else System.getenv("DISCORD_TOKEN")

private suspend fun <E : Event> Flux<E>.addListener(listener: suspend (Flow<E>) -> Unit) = listener.invoke(asFlow())
