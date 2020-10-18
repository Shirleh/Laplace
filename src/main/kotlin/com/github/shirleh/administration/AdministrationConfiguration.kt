package com.github.shirleh.administration

import com.typesafe.config.Config

class AdministrationConfiguration(config: Config) {
    val superuserRoleId: Long = config.getLong("discord.superuserRoleId")
}
