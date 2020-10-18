package com.github.shirleh.administration

import org.koin.dsl.module

val administrationModule = module {
    single(createdAtStart = true) { AdministrationConfiguration(get()) }

    single<ChannelRepository> { ChannelRepositoryImpl() }
}
