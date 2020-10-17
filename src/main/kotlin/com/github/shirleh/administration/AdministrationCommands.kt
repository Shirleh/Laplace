package com.github.shirleh.administration

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.Configuration
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import org.koin.core.inject

class Administration : AbstractCommandCategory(
    help = "Contains commands for administrations. Staff only!",
    name = "admin"
)

class Channel : AbstractCommandCategory(
    help = "Contains commands to manage the channels Laplace listens to",
    name = "channel"
)

private const val OK_HAND_EMOJI = "\uD83D\uDC4C"

class AddChannelCommand : AbstractCommand(name = "add") {

    private val config: Configuration by inject()
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

class RemoveChannelCommand : AbstractCommand(name = "remove") {

    private val config: Configuration by inject()
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
