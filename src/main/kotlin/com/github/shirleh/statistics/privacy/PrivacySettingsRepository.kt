package com.github.shirleh.statistics.privacy

interface PrivacySettingsRepository {
    suspend fun findByUserAndGuild(userId: Long, guildId: Long): PrivacySettings?
    suspend fun save(settings: PrivacySettings): PrivacySettings
    suspend fun delete(settings: PrivacySettings)
}
