package com.github.shirleh.statistics

import com.github.shirleh.persistence.influx.DataPointRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberJoinEvent
import kotlinx.coroutines.flow.*
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
    fun addListener(events: Flow<MemberJoinEvent>) = events
        .buffer()
        .filter { event -> !event.member.isBot }
        .map(MemberJoinDataCollector::toJoinData)
        .onEach { logger.debug { it } }
        .map(JoinData::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

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
