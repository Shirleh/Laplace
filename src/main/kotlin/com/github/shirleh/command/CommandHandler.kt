package com.github.shirleh.command

import arrow.core.Either
import com.github.shirleh.command.cli.CliMessage
import com.github.shirleh.command.cli.laplaceCli
import com.github.shirleh.extensions.await
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

object CommandHandler {

    private val logger = KotlinLogging.logger { }

    /**
     * Listens to incoming [MessageCreateEvent]s and execute the containing command.
     */
    fun addListener(events: Flow<MessageCreateEvent>) = events
        .buffer()
        .filter { event -> event.message.content.startsWith("""<@!${event.client.selfId.asString()}>""") }
        .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
        .onEach { event -> executeCommand(event) }
        .catch { error -> logger.catching(error) }

    private suspend fun executeCommand(event: MessageCreateEvent) {
        logger.entry(event)

        when (val parseResult = laplaceCli().parse(event.message.content)) {
            is Either.Left -> {
                val channel = event.message.channel.await()
                channel.createEmbed { spec -> messageSpec(spec, parseResult.a) }.await()
            }
            is Either.Right -> parseResult.b.execute(event)
        }

        logger.exit()
    }

    private fun messageSpec(spec: EmbedCreateSpec, e: CliMessage) = spec
        .setColor(if (e.error) Color.RED else Color.WHITE)
        .setDescription(e.message)
}
