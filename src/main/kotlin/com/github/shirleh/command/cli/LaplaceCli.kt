package com.github.shirleh.command.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.shirleh.HealthCheck
import com.github.shirleh.PingCommand
import com.github.shirleh.UptimeCommand
import discord4j.core.event.domain.message.MessageCreateEvent

internal class Laplace : AbstractCommand(name = "@Laplace") {
    override suspend fun execute(event: MessageCreateEvent) {
        @Suppress("UNCHECKED_CAST") val parsedCommands = currentContext.obj as List<AbstractCommand>
        parsedCommands.forEach { it.execute(event) }
    }

    override fun run() {
        currentContext.obj = mutableListOf<AbstractCommand>()
    }
}

internal val laplaceCli: () -> Laplace = {
    Laplace().subcommands(
        HealthCheck().subcommands(
            PingCommand(),
            UptimeCommand(),
        ),
    )
}
