package com.github.shirleh.command.cli

import discord4j.core.event.domain.message.MessageCreateEvent

abstract class AbstractCommandCategory(
    help: String = "",
    epilog: String = "",
    name: String? = null,
) : AbstractCommand(
    help,
    epilog,
    name,
) {
    override suspend fun execute(event: MessageCreateEvent) = Unit
}
