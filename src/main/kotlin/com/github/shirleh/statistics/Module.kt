package com.github.shirleh.statistics

import com.github.shirleh.statistics.ban.BanPointRepository
import com.github.shirleh.statistics.ban.BanPointRepositoryImpl
import com.github.shirleh.statistics.emoji.EmojiPointRepository
import com.github.shirleh.statistics.emoji.EmojiPointRepositoryImpl
import com.github.shirleh.statistics.join.JoinPointRepository
import com.github.shirleh.statistics.join.JoinPointRepositoryImpl
import com.github.shirleh.statistics.leave.LeavePointRepository
import com.github.shirleh.statistics.leave.LeavePointRepositoryImpl
import com.github.shirleh.statistics.message.MessagePointRepository
import com.github.shirleh.statistics.message.MessagePointRepositoryImpl
import com.github.shirleh.statistics.nickname.NicknamePointRepository
import com.github.shirleh.statistics.nickname.NicknamePointRepositoryImpl
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepositoryImpl
import com.github.shirleh.statistics.voice.VoicePointRepository
import com.github.shirleh.statistics.voice.VoicePointRepositoryImpl
import org.koin.dsl.module

val statisticsModule = module {
    single<PrivacySettingsRepository> { PrivacySettingsRepositoryImpl() }

    single<BanPointRepository> { BanPointRepositoryImpl(get()) }
    single<EmojiPointRepository> { EmojiPointRepositoryImpl(get()) }
    single<JoinPointRepository> { JoinPointRepositoryImpl(get()) }
    single<LeavePointRepository> { LeavePointRepositoryImpl(get()) }
    single<MessagePointRepository> { MessagePointRepositoryImpl(get()) }
    single<NicknamePointRepository> { NicknamePointRepositoryImpl(get()) }
    single<VoicePointRepository> { VoicePointRepositoryImpl(get()) }
}
