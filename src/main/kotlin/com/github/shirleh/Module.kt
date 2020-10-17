package com.github.shirleh

import com.typesafe.config.ConfigFactory
import org.koin.dsl.module

val mainModule = module {
    single(createdAtStart = true) { ConfigFactory.load() }
    single(createdAtStart = true) { Configuration(get()) }
}
