package com.github.shirleh

import com.github.shirleh.command.addCommandListener
import com.github.shirleh.datacollection.addDataCollectionListeners
import discord4j.core.DiscordClientBuilder
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking<Unit> {
    val token = getToken(args)
    if (token == null) {
        System.err.println("Please provide a Discord token for the bot.")
        exitProcess(1)
    }
    val client = DiscordClientBuilder(token).build()

    addCommandListener(client)
    addDataCollectionListeners(client)

    client.login().awaitFirstOrNull()
}

private fun getToken(args: Array<String>): String? =
    if (args.isNotEmpty()) args[0]
    else System.getenv("DISCORD_TOKEN")
