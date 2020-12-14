package com.github.shirleh.administration

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.command.OK_HAND_EMOJI
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.CategorizableChannel
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Color
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.koin.core.inject

class Administration : AbstractCommandCategory(
    name = "admin",
    help = "Manage Laplace. Staff only!"
)

class Channel : AbstractCommandCategory(
    name = "channel",
    help = """Manage channels.
        
    Configure which channels Laplace will collect its message-driven statistics from."""
)

class ListChannelsCommand : AbstractCommand(
    name = "ls",
    help = """Lists all channels."""
) {
    private val config: AdministrationConfiguration by inject()
    private val channelRepository: ChannelRepository by inject()

    private val channelFilter by option("-f", "--filter").split(",")

    override suspend fun execute(event: MessageCreateEvent) {
        event.member.filter { it.hasPermission(config) }.orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val channels =
            if (channelFilter == null) channelRepository.findAll(guildId)
            else channelRepository.findByIdIn(channelFilter!!.mapNotNull(String::toChannelId))
        val result = channels.map { "<#$it>" }

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

    private val channelMentions by argument().multiple()
    private val categoryId by option("--category", help = "adds all channels under given category").long()

    override suspend fun execute(event: MessageCreateEvent) {
        event.member.filter { it.hasPermission(config) }.orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val channelIdsByCategory = categoryId?.let { getChannelsByCategory(event.guild.await(), it) } ?: emptySet()
        val channelIdsByMentions = channelMentions.mapNotNull(String::toChannelId).toSet()
        val allChannelIds = channelIdsByCategory + channelIdsByMentions
        channelRepository.save(allChannelIds, guildId)

        event.message.addReaction(ReactionEmoji.unicode(OK_HAND_EMOJI)).await()
    }

    private suspend fun getChannelsByCategory(guild: Guild, categoryId: Long): Set<Long> {
        val channel = guild.getChannelById(Snowflake.of(categoryId)).await()
        return if (channel is Category)
            channel.channels.asFlow()
                .map(CategorizableChannel::getId)
                .map(Snowflake::asLong)
                .toSet()
        else
            emptySet()
    }
}

class RemoveChannelCommand : AbstractCommand(
    name = "rm",
    help = """Removes a channel."""
) {
    private val config: AdministrationConfiguration by inject()
    private val channelRepository: ChannelRepository by inject()

    private val channelMentions by argument().multiple()

    override suspend fun execute(event: MessageCreateEvent) {
        event.member.filter { it.hasPermission(config) }.orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val channelIds = channelMentions.mapNotNull(String::toChannelId).toSet()
        channelRepository.delete(channelIds, guildId)

        event.message.addReaction(ReactionEmoji.unicode(OK_HAND_EMOJI)).await()
    }
}

private fun Member.hasPermission(config: AdministrationConfiguration) =
    roleIds.contains(Snowflake.of(config.superuserRoleId))

private fun String.toChannelId() = removePrefix("<#").removeSuffix(">").toLongOrNull()
