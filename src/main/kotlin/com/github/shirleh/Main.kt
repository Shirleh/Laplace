package com.github.shirleh

import com.github.shirleh.command.addCommandListener
import com.github.shirleh.datacollection.addDataCollectionListeners
import discord4j.core.DiscordClientBuilder
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking<Unit> {
    if (args.isEmpty()) {
        System.err.println("Please provide a Discord token for the bot.")
        exitProcess(1)
    }
    val token = args[0]
    val client = DiscordClientBuilder(token).build()

    addCommandListener(client)
    addDataCollectionListeners(client)

    client.login().awaitFirstOrNull()
}
