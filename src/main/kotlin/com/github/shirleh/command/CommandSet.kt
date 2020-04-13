package com.github.shirleh.command

interface CommandSet {
    val commands: Map<String, Command>
}
