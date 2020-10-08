package com.github.shirleh.command.cli

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.ajalt.clikt.core.*
import discord4j.core.event.domain.message.MessageCreateEvent
import mu.KotlinLogging

abstract class AbstractCommand(
    help: String = "",
    epilog: String = "",
    name: String? = null,
) : CliktCommand(
    help,
    epilog,
    name
) {
    private val logger = KotlinLogging.logger { }

    private val invokedCommands by findObject<MutableList<AbstractCommand>>()

    abstract suspend fun execute(event: MessageCreateEvent)

    internal fun parse(message: String): Either<CliMessage, AbstractCommand> {
        val argv = message.split(" ").dropPrefix().also { logger.debug { "argv=$it" } }

        return try {
            parse(argv)
            return this.right()
        } catch (e: PrintHelpMessage) {
            CliMessage(e.command.getFormattedHelp()).left()
        } catch (e: PrintMessage) {
            CliMessage(e.message ?: "").left()
        } catch (e: UsageError) {
            CliMessage(e.helpMessage(), error = true).left()
        } catch (e: CliktError) {
            CliMessage(e.message ?: "", error = true).left()
        } catch (e: Abort) {
            CliMessage(currentContext.localization.aborted(), error = true).left()
        }
    }

    private fun List<String>.dropPrefix() = drop(1)

    override fun run() {
        invokedCommands?.add(this)
    }
}
