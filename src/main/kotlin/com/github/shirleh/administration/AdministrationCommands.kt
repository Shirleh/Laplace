package com.github.shirleh.administration

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Color
import org.koin.core.inject

class Administration : AbstractCommandCategory(
    name = "admin",
    help = "Manage Laplace. Staff only!"
)

class Channel : AbstractCommandCategory(
    name = "channel",
    help = """Manage channels.
        
    Configure which channels Laplace will collect its message-driven statistics from."""
) {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "ls" to listOf("list")
    )
}

private const val OK_HAND_EMOJI = "\uD83D\uDC4C"

class ListChannelsCommand : AbstractCommand(
    name = "list",
    help = """Lists all channels."""
) {
    private val config: AdministrationConfiguration by inject()
    private val channelRepository: ChannelRepository by inject()

    override suspend fun execute(event: MessageCreateEvent) {
        val member = event.member.orElseNull() ?: return
        val superuserRoleId = Snowflake.of(config.superuserRoleId)
        if (!member.roleIds.contains(superuserRoleId)) return

        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val result = channelRepository.findAll(guildId)
            .map { "<#$it>" }

        val channel = event.message.channel.await()
        channel.createEmbed { spec ->
            spec
                .setDescription(result.toString())
                .setColor(Color.WHITE)
        }.await()
    }
}

class AddChannelCommand : AbstractCommand(
    name = "add",
    help = """Adds a channel."""
) {
    private val config: AdministrationConfiguration by inject()
    private val channelRepository: ChannelRepository by inject()

    private val channelId by argument().long()

    override suspend fun execute(event: MessageCreateEvent) {
        val member = event.member.orElseNull() ?: return
        val superuserRoleId = Snowflake.of(config.superuserRoleId)
        if (!member.roleIds.contains(superuserRoleId)) return

        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return
        channelRepository.save(channelId, guildId)

        event.message.addReaction(ReactionEmoji.unicode(OK_HAND_EMOJI)).await()
    }
}

class RemoveChannelCommand : AbstractCommand(
    name = "remove",
    help = """Removes a channel."""
) {
    private val config: AdministrationConfiguration by inject()
    private val channelRepository: ChannelRepository by inject()

    private val channelId by argument().long()

    override suspend fun execute(event: MessageCreateEvent) {
        val member = event.member.orElseNull() ?: return
        val superuserRoleId = Snowflake.of(config.superuserRoleId)
        if (!member.roleIds.contains(superuserRoleId)) return

        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return
        channelRepository.delete(channelId, guildId)

        event.message.addReaction(ReactionEmoji.unicode(OK_HAND_EMOJI)).await()
    }
}
