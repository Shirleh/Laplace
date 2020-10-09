package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberJoinEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object MemberJoinDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects member join data from the incoming [events].
     */
    suspend fun collect(events: Flow<MemberJoinEvent>) {
        events
            .map(::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toDataPoint(event: MemberJoinEvent): Point {
        logger.entry(event)

        val member = event.member

        val guildId = event.guildId.asString()
        val guildMemberId = member.id.asString()
        val creationDate = member.id.timestamp.epochSecond
        val isBot = member.isBot
        val timestamp = member.joinTime

        val result = Point.measurement("guildMembership")
            .addTag("event", "join")
            .addTag("guildId", guildId)
            .addTag("guildMemberId", guildMemberId)
            .addField("creationDate", creationDate)
            .addField("isBot", isBot)
            .time(timestamp, WritePrecision.S)

        return logger.exit(result)
    }
}
