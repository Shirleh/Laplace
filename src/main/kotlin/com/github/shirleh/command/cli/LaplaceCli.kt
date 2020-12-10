package com.github.shirleh.command.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.shirleh.administration.*
import com.github.shirleh.healthcheck.HealthCheck
import com.github.shirleh.healthcheck.PingCommand
import com.github.shirleh.healthcheck.ShowInfluxHealthCommand
import com.github.shirleh.healthcheck.UptimeCommand
import com.github.shirleh.statistics.ban.BanStatisticsCommands
import com.github.shirleh.statistics.ban.ShowBanCountCommand
import com.github.shirleh.statistics.ban.ShowBanRankingCommand
import com.github.shirleh.statistics.emoji.EmojiStatistics
import com.github.shirleh.statistics.emoji.TopEmojiCommand
import com.github.shirleh.statistics.message.ActivityCommand
import com.github.shirleh.statistics.message.MessageStatistic
import com.github.shirleh.statistics.privacy.ListPrivacySettingsCommand
import com.github.shirleh.statistics.privacy.OptInCommand
import com.github.shirleh.statistics.privacy.OptOutCommand
import com.github.shirleh.statistics.privacy.PrivacyCommands
import discord4j.core.event.domain.message.MessageCreateEvent

internal class Laplace : AbstractCommand(name = "@Laplace") {
    override suspend fun execute(event: MessageCreateEvent) {
        @Suppress("UNCHECKED_CAST") val parsedCommands = currentContext.obj as List<AbstractCommand>
        parsedCommands.forEach { it.execute(event) }
    }

    override fun run() {
        currentContext.obj = mutableListOf<AbstractCommand>()
    }
}

internal val laplaceCli: () -> Laplace = {
    Laplace().subcommands(
        BanStatisticsCommands().subcommands(
            ShowBanRankingCommand(),
            ShowBanCountCommand()
        ),
        EmojiStatistics().subcommands(
            TopEmojiCommand()
        ),
        MessageStatistic().subcommands(
            ActivityCommand()
        ),
        PrivacyCommands().subcommands(
            ListPrivacySettingsCommand(),
            OptInCommand(),
            OptOutCommand()
        ),
        Administration().subcommands(
            Channel().subcommands(
                ListChannelsCommand(),
                AddChannelCommand(),
                RemoveChannelCommand(),
            )
        ),
        HealthCheck().subcommands(
            PingCommand(),
            UptimeCommand(),
            ShowInfluxHealthCommand(),
        ),
    )
}
