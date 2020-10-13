package com.github.shirleh.administration

import org.jetbrains.exposed.sql.Table

object Guilds : Table() {
    val id = long("guild_id")
    override val primaryKey = PrimaryKey(id)
}
