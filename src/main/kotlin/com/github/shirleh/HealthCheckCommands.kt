package com.github.shirleh

import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactive.awaitFirst
import java.lang.management.ManagementFactory
import java.time.Duration

class HealthCheck : AbstractCommandCategory(name = "health")

class PingCommand : AbstractCommand() {
    override suspend fun execute(event: MessageCreateEvent) {
        val channel = event.message.channel.awaitFirst()
        channel.createMessage("Pong!").awaitFirst()
    }
}

class UptimeCommand : AbstractCommand() {
    override suspend fun execute(event: MessageCreateEvent) {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        val duration = Duration.ofMillis(uptime)
        val days = duration.toDaysPart()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        val channel = event.message.channel.awaitFirst()
        channel.createMessage("""$days day(s), $hours hour(s), $minutes minute(s), $seconds second(s)""").awaitFirst()
    }
}
