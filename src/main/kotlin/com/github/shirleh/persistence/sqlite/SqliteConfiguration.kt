package com.github.shirleh.persistence.sqlite

import com.typesafe.config.Config

class SqliteConfiguration(config: Config) {
    val url: String = config.getString("sqlite.url")
}
