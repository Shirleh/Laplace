package com.github.shirleh.statistics

import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.core.event.domain.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Duration
import java.time.Instant

private data class LeaveData(
    val guildId: String,
    val userId: String?,
    val membershipDuration: Duration,
    val isBoosting: Boolean,
    val leaveTime: Instant
) {
    fun toDataPoint() = Point.measurement("member_leave")
        .addTag("guildId", guildId)
        .addTag("userId", userId ?: "")
        .addField("membershipDuration", membershipDuration.seconds)
        .addField("isBoosting", isBoosting)
        .time(leaveTime, WritePrecision.MS)
}

object MemberLeaveDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    /**
     * Collects member leave data from the incoming [events].
     */
    fun addListener(events: Flow<MemberLeaveEvent>) = events
        .buffer()
        .mapNotNull(MemberLeaveDataCollector::toLeaveData)
        .map(LeaveData::toDataPoint)
        .onEach { logger.debug { it } }
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }


    private suspend fun toLeaveData(event: MemberLeaveEvent): LeaveData? {
        logger.entry(event)

        val member = event.member.orElseNull() ?: return run {
            logger.error { "Member not found in cache. Data permanently lost. Should not happen with the right intents." }
            null
        }
        val userId = member.id
        val joinTime = member.joinTime
        val leaveTime = Instant.now()

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(userId.asLong(), event.guildId.asLong())

        val result = LeaveData(
            guildId = event.guildId.asString(),
            userId = if (privacySettings?.membership == true) userId.asString() else null,
            membershipDuration = Duration.between(joinTime, leaveTime),
            isBoosting = member.premiumTime.isPresent,
            leaveTime = leaveTime
        )

        return logger.exit(result)
    }
}
