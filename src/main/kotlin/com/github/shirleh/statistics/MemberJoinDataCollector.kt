package com.github.shirleh.statistics

import com.github.shirleh.persistence.influx.DataPointRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberJoinEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class JoinData(
    val guildId: String,
    val creationDate: Instant,
    val isBoosting: Boolean,
    val joinTime: Instant,
) {
    fun toDataPoint() = Point.measurement("member_join")
        .addTag("guildId", guildId)
        .addField("creationDate", creationDate.epochSecond)
        .addField("isBoosting", isBoosting)
        .time(joinTime, WritePrecision.S)
}

object MemberJoinDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects member join data from the incoming [events].
     */
    suspend fun collect(events: Flow<MemberJoinEvent>) {
        events
            .filter { event -> !event.member.isBot }
            .map(MemberJoinDataCollector::toJoinData)
            .map(JoinData::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toJoinData(event: MemberJoinEvent): JoinData {
        logger.entry(event)

        val member = event.member

        val result = JoinData(
            guildId = event.guildId.asString(),
            creationDate = member.id.timestamp,
            isBoosting = member.premiumTime.isPresent,
            joinTime = member.joinTime
        )

        return logger.exit(result)
    }
}
