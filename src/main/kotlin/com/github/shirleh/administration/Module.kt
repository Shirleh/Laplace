package com.github.shirleh.administration

import org.koin.dsl.module

val administrationModule = module {
    single<ChannelRepository> { ChannelRepositoryImpl() }
}
