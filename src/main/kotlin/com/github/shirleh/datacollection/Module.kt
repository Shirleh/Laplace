package com.github.shirleh.datacollection

import org.koin.dsl.module

val dataCollectionModule = module {
    single<DataPointRepository> { DataPointRepositoryImpl() }
}
