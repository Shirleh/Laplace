package com.github.shirleh.statistics.privacy

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = long("user_id")
    override val primaryKey = PrimaryKey(id)
}
