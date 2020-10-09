package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Duration
import java.time.Instant

object MemberLeaveDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects member leave data from the incoming [events].
     */
    suspend fun collect(events: Flow<MemberLeaveEvent>) {
        events
            .map(::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toDataPoint(event: MemberLeaveEvent): Point {
        logger.entry(event)

        val guildMemberId = event.user.id.asString()
        val guildId = event.guildId.asString()
        val joinTime = event.member.map { it.joinTime }
            .orElseThrow { IllegalStateException("Member not present in MemberLeaveEvent?") }
        val leaveTime = Instant.now()
        val membershipDuration = Duration.between(joinTime, leaveTime)

        val result = Point.measurement("guildMembership")
            .addTag("event", "leave")
            .addTag("guildId", guildId)
            .addTag("guildMemberId", guildMemberId)
            .addField("membershipDuration", membershipDuration.seconds)
            .time(leaveTime, WritePrecision.S)

        return logger.exit(result)
    }
}
