package com.github.shirleh.command

import com.github.shirleh.healthcheck.HealthCheckCommandSet
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

object CommandHandler {

    private val commandRepository = CommandRegistry
        .register(HealthCheckCommandSet)

    /**
     * Listens to commands from the incoming [flow] of [MessageCreateEvent]s.
     */
    suspend fun executeCommands(flow: Flow<MessageCreateEvent>) = flow
        .filter { it.message.isAuthorHuman() }
        .filter { it.message.containsPrefix("""<@!${it.client.selfId.asString()}>""") }
        .collect { event ->
            val parser = event.message.content.let { CommandParser(it) }
            parser.next() // skips prefix

            val commandName = parser.next() ?: return@collect
            val command = commandRepository.findByName(commandName) ?: return@collect

            command.handler.invoke(parser, event)
        }

    private fun Message.isAuthorHuman(): Boolean = author.map { !it.isBot }.orElse(false)

    private fun Message.containsPrefix(prefix: String) = content.substringBefore(' ') == prefix
}
