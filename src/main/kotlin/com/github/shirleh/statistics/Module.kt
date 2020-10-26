package com.github.shirleh.statistics

import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepositoryImpl
import org.koin.dsl.module

val statisticsModule = module {
    single<PrivacySettingsRepository> { PrivacySettingsRepositoryImpl() }
}
