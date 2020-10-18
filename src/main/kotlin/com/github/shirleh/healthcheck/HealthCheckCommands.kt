package com.github.shirleh.healthcheck

import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import discord4j.core.event.domain.message.MessageCreateEvent
import java.lang.management.ManagementFactory
import java.time.Duration

class HealthCheck : AbstractCommandCategory(name = "health")

class PingCommand : AbstractCommand(
    name = "ping",
    help = """Replies with "Pong!"."""
) {
    override suspend fun execute(event: MessageCreateEvent) {
        val channel = event.message.channel.await()
        channel.createMessage("Pong!").await()
    }
}

class UptimeCommand : AbstractCommand(
    name = "uptime",
    help = """Shows the amount of time Laplace is up and running."""
) {
    override suspend fun execute(event: MessageCreateEvent) {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        val duration = Duration.ofMillis(uptime)
        val days = duration.toDaysPart()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        val channel = event.message.channel.await()
        channel.createMessage("""$days day(s), $hours hour(s), $minutes minute(s), $seconds second(s)""").await()
    }
}
