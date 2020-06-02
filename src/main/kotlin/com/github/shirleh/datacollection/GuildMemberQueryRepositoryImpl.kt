package com.github.shirleh.datacollection

import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import mu.KotlinLogging
import java.time.*

private val PAST = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0).toInstant(ZoneOffset.UTC)

/**
 * Default implementation of [GuildMemberQueryRepository].
 */
class GuildMemberQueryRepositoryImpl : GuildMemberQueryRepository {

    private val logger = KotlinLogging.logger { }

    private val influxDBClient = InfluxDBClientKotlinFactory.create()

    override suspend fun findLatestJoinDate(guildMemberId: String, guildId: String): Instant? {
        logger.entry(guildMemberId)

        val restrictions = Restrictions.and(
            Restrictions.measurement().equal("guildMember"),
            Restrictions.tag("guildMemberId").equal(guildMemberId),
            Restrictions.tag("guildId").equal(guildId),
            Restrictions.tag("event").equal("join")
        )
        val query = Flux.from("data")
            .unboundedRange()
            .filter(restrictions)
            .last()

        val results = influxDBClient.getQueryKotlinApi().query(query.toString())
        val record = results.receive()

        return record.time?.also { logger.exit(it) }
    }

    /**
     * Does not filter the results by time boundaries.
     *
     * Normally, InfluxDB require queries to be bound by time to prevent unnecessarily complex (expensive) queries.
     * This function works around that by creating a range with a start time far in the past, essentially creating an
     * unbounded range.
     *
     * Please use this only when necessary!
     */
    private fun Flux.unboundedRange() = this.range(PAST)
}
