package com.github.shirleh.administration

internal interface ChannelRepository {
    suspend fun findByIdIn(ids: Collection<Long>): Set<Long>
    suspend fun findAll(guildId: Long): Set<Long>
    suspend fun save(channelId: Long, guildId: Long)
    suspend fun save(channelIds: Set<Long>, guildId: Long)
    suspend fun delete(channelId: Long, guildId: Long)
    suspend fun delete(channelIds: Set<Long>, guildId: Long)
}
