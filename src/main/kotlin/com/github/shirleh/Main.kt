package com.github.shirleh

import com.github.shirleh.administration.administrationModule
import com.github.shirleh.command.CommandHandler
import com.github.shirleh.persistence.influx.influxModule
import com.github.shirleh.persistence.sqlite.sqliteModule
import com.github.shirleh.statistics.ban.BanDataCollector
import com.github.shirleh.statistics.emoji.MessageEmojiDataCollector
import com.github.shirleh.statistics.emoji.ReactionEmojiDataCollector
import com.github.shirleh.statistics.join.JoinDataCollector
import com.github.shirleh.statistics.leave.LeaveDataCollector
import com.github.shirleh.statistics.message.MessageDataCollector
import com.github.shirleh.statistics.nickname.NicknameDataCollector
import com.github.shirleh.statistics.statisticsModule
import com.github.shirleh.statistics.voice.VoiceDataCollector
import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.BanEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.MemberUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
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
        modules(
            mainModule, influxModule, sqliteModule,
            administrationModule, statisticsModule
        )
    }

    DiscordClient.create(token).withGateway { client ->
        mono {
            client.on(MessageCreateEvent::class.java).asFlow()
                .let(CommandHandler::addListener)
                .launchIn(this)

            client.on(MessageCreateEvent::class.java).asFlow()
                .let(MessageDataCollector::addListener)
                .launchIn(this)

            client.on(MessageCreateEvent::class.java).asFlow()
                .let(MessageEmojiDataCollector::addListener)
                .launchIn(this)

            client.on(ReactionAddEvent::class.java).asFlow()
                .let(ReactionEmojiDataCollector::addListener)
                .launchIn(this)

            client.on(MemberJoinEvent::class.java).asFlow()
                .let(JoinDataCollector::addListener)
                .launchIn(this)

            client.on(MemberLeaveEvent::class.java).asFlow()
                .let(LeaveDataCollector::addListener)
                .launchIn(this)

            client.on(MemberUpdateEvent::class.java).asFlow()
                .let(NicknameDataCollector::addListener)
                .launchIn(this)

            client.on(BanEvent::class.java).asFlow()
                .let(BanDataCollector::addListener)
                .launchIn(this)

            client.on(VoiceStateUpdateEvent::class.java).asFlow()
                .let(VoiceDataCollector::addListener)
                .launchIn(this)
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
