package com.github.shirleh.datacollection

import java.time.Instant

/**
 * Repository interface for query operations on guild members.
 */
interface GuildMemberQueryRepository {

    /**
     * Returns the latest point in time at which guild member [guildMemberId] joined guild [guildId].
     */
    suspend fun findLatestJoinDate(guildMemberId: String, guildId: String): Instant?
}
