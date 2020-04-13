package com.github.shirleh.command

object CommandRegistry {

    private val commands = mutableMapOf<String, Command>()

    fun findByName(name: String) = commands[name]

    fun register(commandSet: CommandSet): CommandRegistry {
        commands.putAll(commandSet.commands)
        return this
    }
}
