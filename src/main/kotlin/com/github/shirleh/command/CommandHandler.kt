package com.github.shirleh.command

import arrow.core.Either
import com.github.shirleh.command.cli.CliMessage
import com.github.shirleh.command.cli.laplaceCli
import com.github.shirleh.extensions.await
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

object CommandHandler {

    /**
     * Listens to incoming [MessageCreateEvent]s and execute the containing command.
     */
    suspend fun executeCommands(flow: Flow<MessageCreateEvent>) = flow
        .filter { it.message.hasHumanAuthor() }
        .filter { it.message.containsPrefix("""<@!${it.client.selfId.asString()}>""") }
        .collect {
            when (val parseResult = laplaceCli().parse(it.message.content)) {
                is Either.Left -> {
                    val channel = it.message.channel.await()
                    channel.createEmbed { spec -> messageSpec(spec, parseResult.a) }.await()
                }
                is Either.Right -> parseResult.b.execute(it)
            }
        }

    private fun Message.hasHumanAuthor(): Boolean = author.map { !it.isBot }.orElse(false)

    private fun Message.containsPrefix(prefix: String) = content.startsWith(prefix)

    private fun messageSpec(spec: EmbedCreateSpec, e: CliMessage) = spec
        .setColor(if (e.error) Color.RED else Color.WHITE)
        .setDescription(e.message)
}
