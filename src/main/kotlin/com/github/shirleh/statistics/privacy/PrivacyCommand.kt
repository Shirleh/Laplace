package com.github.shirleh.statistics.privacy

import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.shirleh.command.OK_HAND_EMOJI
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging
import org.koin.core.inject

class PrivacyCommands : AbstractCommandCategory(
    name = "privacy",
    help = """Manage privacy (see info in --help)

    By default, Laplace collects statistics such that nothing can be traced back to an individual. Laplace provides 
    fine-grained opt-in options for you to give Laplace permission to tag your unique Discord ID to 
    the (subset of) data it collects. This enables Laplace to give you a personal view of various statistics.
    
    Statistics in Laplace are divided in so-called *measurements* to which you can opt-in:
    
    message - message count, message length, word count
    
    emoji - emoji count (from messages and reactions)
    
    voice - voice, mute, and deaf durations
    
    nickname - list of past nicknames
    
    membership - join and leave frequency, as well as membership duration"""
)

private val MEASUREMENT_NAMES = listOf("message", "emoji", "voice", "nickname", "membership")
    .fold(StringBuilder()) { acc, s -> acc.appendLine(s) }.toString()
private val DEFAULT_MEASUREMENT_VALUES = MEASUREMENT_NAMES
    .fold(StringBuilder()) { acc, _ -> acc.appendLine(false) }.toString()

class ListPrivacySettingsCommand : AbstractCommand(
    name = "ls",
    help = """List current privacy settings"""
) {
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    override suspend fun execute(event: MessageCreateEvent) {
        val userId = event.member.map(Member::getId).map(Snowflake::asLong).orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val result = privacySettingsRepository.findByUserAndGuild(userId, guildId)

        event.message.channel.await()
            .createEmbed { spec ->
                spec.addField("measurements", MEASUREMENT_NAMES, true)
                    .addField("opt-in", result?.values ?: DEFAULT_MEASUREMENT_VALUES, true)
            }
            .await()
    }
}

class OptInCommand : AbstractCommand(
    name = "opt-in",
    help = """Opt-in to given measurements"""
) {
    private val logger = KotlinLogging.logger { }

    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    private val measurements by option("-m", "--measurements")
        .choice(
            Measurement.MESSAGE.asOption(),
            Measurement.EMOJI.asOption(),
            Measurement.VOICE.asOption(),
            Measurement.NICKNAME.asOption(),
            Measurement.MEMBERSHIP.asOption(),
        )
        .multiple()

    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val userId = event.member.map(Member::getId).map(Snowflake::asLong).orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(userId, guildId) ?: PrivacySettings(userId, guildId)
        val result = privacySettings
            .optIn(measurements)
            .run { privacySettingsRepository.save(this) }

        event.message.channel.await()
            .createEmbed { spec ->
                spec.addField("measurements", MEASUREMENT_NAMES, true)
                    .addField("opt-in", result.values, true)
            }
            .await()
        logger.exit()
    }
}

class OptOutCommand : AbstractCommand(
    name = "opt-out",
    help = """Opt-out of given measurements"""
) {
    private val logger = KotlinLogging.logger { }

    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    private val measurements by option("-m", "--measurements")
        .choice(
            Measurement.MESSAGE.asOption(),
            Measurement.EMOJI.asOption(),
            Measurement.VOICE.asOption(),
            Measurement.NICKNAME.asOption(),
            Measurement.MEMBERSHIP.asOption(),
        )
        .multiple(required = true)

    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val userId = event.member.map(Member::getId).map(Snowflake::asLong).orElseNull() ?: return
        val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return

        val privacySettings = privacySettingsRepository.findByUserAndGuild(userId, guildId)
        val result = privacySettings
            ?.optOut(measurements)
            ?.run { privacySettingsRepository.save(this) }

        event.message.channel.await()
            .createEmbed { spec ->
                spec.addField("measurements", MEASUREMENT_NAMES, true)
                    .addField("opt-in", result?.values ?: DEFAULT_MEASUREMENT_VALUES, true)
            }
            .await()
        logger.exit()
    }
}
