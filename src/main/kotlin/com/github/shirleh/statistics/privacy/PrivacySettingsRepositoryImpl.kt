package com.github.shirleh.statistics.privacy

import com.github.shirleh.extensions.awaitNullable
import discord4j.store.jdk.JdkStore
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PrivacySettingsRepositoryImpl : PrivacySettingsRepository {

    private val cache = JdkStore<Long, PrivacySettings>(false)

    override suspend fun findByUserAndGuild(userId: Long, guildId: Long): PrivacySettings? {
        return cache.find(userId).awaitNullable() ?: newSuspendedTransaction {
            PrivacySettingsTable
                .select { PrivacySettingsTable.user eq userId and (PrivacySettingsTable.guild eq guildId) }
                .map {
                    PrivacySettings(
                        userId = userId,
                        guildId = guildId,
                        message = it[PrivacySettingsTable.message],
                        emoji = it[PrivacySettingsTable.emoji],
                        voice = it[PrivacySettingsTable.voice],
                        nickname = it[PrivacySettingsTable.nickname],
                        membership = it[PrivacySettingsTable.membership],
                    )
                }
                .firstOrNull()
        }?.also { cache.save(it.userId, it) }
    }

    override suspend fun save(settings: PrivacySettings): PrivacySettings {
        newSuspendedTransaction {
            PrivacySettingsEntity
                .find { PrivacySettingsTable.user eq settings.userId and (PrivacySettingsTable.guild eq settings.guildId) }
                .firstOrNull()
                ?.apply {
                    message = settings.message
                    emoji = settings.emoji
                    voice = settings.voice
                    nickname = settings.nickname
                    membership = settings.membership
                }
                ?: PrivacySettingsEntity.new {
                    message = settings.message
                    emoji = settings.emoji
                    voice = settings.voice
                    nickname = settings.nickname
                    membership = settings.membership
                }
        }

        return settings
    }

    override suspend fun delete(settings: PrivacySettings) {
        PrivacySettingsEntity
            .find { PrivacySettingsTable.user eq settings.userId and (PrivacySettingsTable.guild eq settings.guildId) }
            .forEach(PrivacySettingsEntity::delete)
    }
}
