package com.github.shirleh.administration

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction
import java.util.concurrent.ConcurrentHashMap

class ChannelRepositoryImpl : ChannelRepository {

    private val _cache: MutableMap<Long, MutableSet<Long>> = ConcurrentHashMap()

    override suspend fun findAll(guildId: Long): Set<Long> = _cache[guildId] ?: run { populateCache(guildId) }

    private suspend fun populateCache(guildId: Long): MutableSet<Long> {
        val result = newSuspendedTransaction(Dispatchers.IO) {
            Channels.select { Channels.guild eq guildId }
                .map { it[Channels.id] }
                .toMutableSet()
        }
        _cache[guildId] = result
        return result
    }

    override suspend fun save(channelId: Long, guildId: Long) {
        newSuspendedTransaction {
            suspendedTransaction {
                val count = Guilds.select { Guilds.id eq guildId }.count()
                if (count == 0L) {
                    Guilds.insert { it[id] = guildId }
                }
            }

            Channels.insert {
                it[id] = channelId
                it[guild] = guildId
            }
        }

        val channels = _cache[guildId] ?: return
        channels.add(channelId)
        _cache[guildId] = channels
    }

    override suspend fun delete(channelId: Long, guildId: Long) {
        newSuspendedTransaction {
            Channels.deleteWhere { Channels.id eq channelId }
        }

        val channels = _cache[guildId] ?: return
        channels.remove(channelId)
        _cache[guildId] = channels
    }
}