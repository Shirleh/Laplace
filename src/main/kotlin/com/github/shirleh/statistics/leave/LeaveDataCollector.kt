package com.github.shirleh.statistics.leave

import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.core.event.domain.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Duration
import java.time.Instant

object LeaveDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val leavePointRepository: LeavePointRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    /**
     * Collects member leave data from the incoming [events].
     */
    fun addListener(events: Flow<MemberLeaveEvent>) = events
        .buffer()
        .mapNotNull(this::aggregateLeaveData)
        .onEach(leavePointRepository::save)
        .catch { error -> logger.catching(error) }


    private suspend fun aggregateLeaveData(event: MemberLeaveEvent): LeavePoint? {
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

        val result = LeavePoint(
            guildId = event.guildId.asString(),
            userId = if (privacySettings?.membership == true) userId.asString() else null,
            membershipDuration = Duration.between(joinTime, leaveTime),
            isBoosting = member.premiumTime.isPresent,
            leaveTime = leaveTime
        )

        return logger.exit(result)
    }
}
