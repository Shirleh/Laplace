package com.github.shirleh.command

import com.github.shirleh.healthcheck.HealthCheckCommandSet
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import mu.KotlinLogging

object CommandHandler {

    private val logger = KotlinLogging.logger { }

    private val commandRepository = CommandRegistry
        .register(HealthCheckCommandSet)

    /**
     * Listens to commands from the incoming [flow] of [MessageCreateEvent]s.
     */
    suspend fun executeCommands(flow: Flow<MessageCreateEvent>) = flow
        .filter { it.message.isAuthorHuman() }
        .filter { it.message.containsPrefix("""<@!${it.client.selfId.asString()}>""") }
        .collect { executeCommand(it) }

    private suspend fun executeCommand(event: MessageCreateEvent) {
        logger.entry(event)

        val parser = event.message.content.let { CommandParser(it) }
        parser.next() // skips prefix

        val commandName = parser.next() ?: return
        val command = commandRepository.findByName(commandName) ?: return
        command.handler.invoke(parser, event)

        logger.exit()
    }

    private fun Message.isAuthorHuman(): Boolean = author.map { !it.isBot }.orElse(false)

    private fun Message.containsPrefix(prefix: String) = content.substringBefore(' ') == prefix
}
