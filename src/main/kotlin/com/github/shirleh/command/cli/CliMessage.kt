package com.github.shirleh.command.cli

/**
 * Conveys a short-circuited message from the CLI, usually help or error messages.
 */
internal class CliMessage(val message: String, val error: Boolean = false)
