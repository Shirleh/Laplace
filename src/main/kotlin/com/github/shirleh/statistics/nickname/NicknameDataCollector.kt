package com.github.shirleh.statistics.nickname

import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.statistics.AUDIT_LOG_UPDATE_DELAY
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.core.`object`.audit.ActionType
import discord4j.core.`object`.audit.ChangeKey
import discord4j.core.event.domain.guild.MemberUpdateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object NicknameDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val nicknamePointRepository: NicknamePointRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()

    /**
     * Collects member nickname data from the incoming [events].
     */
    fun addListener(events: Flow<MemberUpdateEvent>) = events
        .buffer()
        .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
        .mapNotNull(this::aggregateNicknameData)
        .onEach(nicknamePointRepository::save)
        .catch { error -> logger.catching(error) }


    private suspend fun aggregateNicknameData(event: MemberUpdateEvent): NicknamePoint? {
        logger.entry(event)

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(event.memberId.asLong(), event.guildId.asLong())

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_UPDATE) }
            .asFlow()
            .take(10)
            .mapNotNull { auditLogEntry ->
                auditLogEntry
                    .getChange(ChangeKey.USER_NICK)
                    .map {
                        val userId =
                            if (privacySettings?.nickname == true) auditLogEntry.responsibleUserId.asString()
                            else null

                        NicknamePoint(
                            userId = userId,
                            newNickname = it.currentValue.orElse(""),
                            hadNickname = it.oldValue.isPresent,
                        )
                    }
                    .orElseNull()
            }
            .filter { change -> event.currentNickname.map { it == change.newNickname }.orElse(false) }
            .firstOrNull()

        return logger.exit(result)
    }
}
