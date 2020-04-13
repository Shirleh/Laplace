package com.github.shirleh.command

import discord4j.core.event.domain.message.MessageCreateEvent

class Command(
    val description: String,
    val usage: String = "",
    val handler: suspend (CommandParser, MessageCreateEvent) -> Unit
)
