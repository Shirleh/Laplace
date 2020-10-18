package com.github.shirleh

import com.github.shirleh.administration.Channels
import com.github.shirleh.administration.Guilds
import com.github.shirleh.administration.administrationModule
import com.github.shirleh.command.CommandHandler
import com.github.shirleh.datacollection.*
import com.github.shirleh.datacollection.emoji.EmojiDataCollector
import com.github.shirleh.datacollection.MessageDataCollector
import com.github.shirleh.persistence.influx.influxModule
import discord4j.core.DiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.BanEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
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

    startKoin {
        modules(mainModule, influxModule, administrationModule, dataCollectionModule)
    }

    Database.connect("jdbc:sqlite:./data/data.db", "org.sqlite.JDBC")
    transaction { SchemaUtils.createMissingTablesAndColumns(Guilds, Channels) }

    DiscordClient.create(token).withGateway { client ->
        mono {
            launch { client.on(MessageCreateEvent::class.java).addListener(CommandHandler::executeCommands) }

            launch { client.on(MessageCreateEvent::class.java).addListener(MessageDataCollector::collect) }

            launch { client.on(MessageCreateEvent::class.java).addListener(EmojiDataCollector::collectFromMessages) }
            launch { client.on(ReactionAddEvent::class.java).addListener(EmojiDataCollector::collectFromReactionAdds) }

            launch { client.on(MemberJoinEvent::class.java).addListener(MemberJoinDataCollector::collect) }
            launch { client.on(MemberLeaveEvent::class.java).addListener(MemberLeaveDataCollector::collect) }
            launch { client.on(MemberUpdateEvent::class.java).addListener(NicknameDataCollector::collect) }
            launch { client.on(BanEvent::class.java).addListener(BanDataCollector::collect) }

            launch { client.on(VoiceStateUpdateEvent::class.java).addListener(VoiceDataCollector::collect) }
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
