package com.github.shirleh

import com.typesafe.config.ConfigFactory
import org.koin.dsl.module

val mainModule = module {
    single(createdAtStart = true) {
        val dev = ConfigFactory.load()
        val prod = ConfigFactory.load("application.prod")
        val result = prod.withFallback(dev)
        Configuration(result)
    }
}
