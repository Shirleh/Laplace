package com.github.shirleh.command

import com.github.shirleh.monitoring.MonitoringCommandSet
import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow

private val commandRepository = CommandRegistry
    .register(MonitoringCommandSet)

fun addCommandListener(client: DiscordClient) {
    client.eventDispatcher.on(MessageCreateEvent::class.java).asFlow()
        .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
        .filter { event ->
            val mention = event.message.content.map { it.substringBefore(' ') }.orElse(null) ?: return@filter false
            val selfId = client.selfId.map { it.asString() }.orElse(null) ?: return@filter false

            mention == """<@!$selfId>"""
        }
        .onEach { event ->
            val parser = event.message.content.map { CommandParser(it) }.orElse(null) ?: return@onEach
            parser.next() // skips mention
            val commandName = parser.next() ?: return@onEach
            val command = commandRepository.findByName(commandName) ?: return@onEach
            command.handler.invoke(parser, event)
        }
        .launchIn(GlobalScope)
}
