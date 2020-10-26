package com.github.shirleh.persistence.sqlite

import com.github.shirleh.administration.Channels
import com.github.shirleh.administration.Guilds
import com.github.shirleh.statistics.privacy.PrivacySettingsTable
import com.github.shirleh.statistics.privacy.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module

val sqliteModule = module {
    single(createdAtStart = true) { SqliteConfiguration(get()) }
    single(createdAtStart = true) { initSqlite(get()) }
}

private fun initSqlite(config: SqliteConfiguration): Database {
    val result = Database.connect(config.url, "org.sqlite.JDBC")
    transaction { SchemaUtils.createMissingTablesAndColumns(Guilds, Channels, Users, PrivacySettingsTable) }
    return result
}
