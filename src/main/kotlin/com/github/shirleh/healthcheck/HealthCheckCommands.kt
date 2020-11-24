package com.github.shirleh.healthcheck

import com.github.shirleh.command.OK_HAND_EMOJI
import com.github.shirleh.command.SCREAM_EMOJI
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.HealthCheck
import discord4j.core.event.domain.message.MessageCreateEvent
import org.koin.core.inject
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

        event.message.channel.await()
            .createMessage("$days day(s), $hours hour(s), $minutes minute(s), $seconds second(s)").await()
    }
}

class ShowInfluxHealthCommand : AbstractCommand(
    name = "influx",
    help = """Shows the health of InfluxDB."""
) {
    private val influxDBClient: InfluxDBClient by inject()

    override suspend fun execute(event: MessageCreateEvent) {
        val message = when (influxDBClient.health().status) {
            HealthCheck.StatusEnum.PASS -> OK_HAND_EMOJI
            HealthCheck.StatusEnum.FAIL -> SCREAM_EMOJI
            null -> "null" // should never happen
        }

        event.message.channel.await()
            .createMessage(message).await()
    }
}
