package com.github.shirleh.administration

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Channels : Table() {
    val id = long("channel_id")
    val guild = reference("guild", Guilds.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(id)
}
