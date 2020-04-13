package com.github.shirleh.monitoring

import com.github.shirleh.command.Command
import com.github.shirleh.command.CommandSet
import kotlinx.coroutines.reactive.awaitFirst
import java.lang.management.ManagementFactory
import java.time.Duration

object MonitoringCommandSet : CommandSet {

    override val commands: Map<String, Command> = mutableMapOf<String, Command>().apply {
        this["ping"] = Command(description = """Responds with "Pong!".""",
            handler = { _, event ->
                val channel = event.message.channel.awaitFirst()
                channel.createMessage("Pong!").awaitFirst()
            }
        )

        this["uptime"] = Command(description = """Responds with the total uptime of this bot.""",
            handler = { _, event ->
                val channel = event.message.channel.awaitFirst()

                val uptime = ManagementFactory.getRuntimeMXBean().uptime
                val duration = Duration.ofMillis(uptime)

                val days = duration.toDaysPart()
                val hours = duration.toHoursPart()
                val minutes = duration.toMinutesPart()
                val seconds = duration.toSecondsPart()

                val message = """$days day(s), $hours hour(s), $minutes minute(s), $seconds second(s)"""
                channel.createMessage(message).awaitFirst()
            }
        )
    }
}
