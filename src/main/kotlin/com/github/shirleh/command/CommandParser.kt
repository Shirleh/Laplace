package com.github.shirleh.command

import java.util.*

class CommandParser(private val input: String) {

    private val scanner = Scanner(input)

    fun asString(): String? = if (scanner.hasNext()) scanner.nextLine() else ""
    fun split(delimiter: String = " ") = input.split(delimiter)

    fun next(): String? = if (scanner.hasNext()) scanner.next() else null
    fun nextLong(): Long? = if (scanner.hasNextLong()) scanner.nextLong() else null
}