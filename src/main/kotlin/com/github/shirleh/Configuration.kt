package com.github.shirleh

import com.typesafe.config.Config

class Configuration(config: Config) {
    val superuserRoleId: Long = config.getLong("discord.superuserRoleId")
}
