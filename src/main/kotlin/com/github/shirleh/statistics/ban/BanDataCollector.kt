package com.github.shirleh.statistics.ban

import com.github.shirleh.extensions.await
import com.github.shirleh.statistics.AUDIT_LOG_UPDATE_DELAY
import discord4j.core.`object`.audit.ActionType
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.guild.BanEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object BanDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val banPointRepository: BanPointRepository by inject()

    /**
     * Collects ban data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
    fun addListener(events: Flow<BanEvent>) = events
        .buffer()
        .flatMapConcat { event ->
            flowOf(event)
                .mapNotNull(this::findBanAuthorId)
                .map { author -> BanPoint(event.guildId, author.id, author.isBot) }
                .onEach(banPointRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private suspend fun findBanAuthorId(event: BanEvent): User? {
        logger.entry(event)

        delay(AUDIT_LOG_UPDATE_DELAY)

        val banAuthorId = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD) }
            .asFlow()
            .take(10)
            .filter { auditLogEntry -> auditLogEntry.targetId.orElse(null) == event.user.id }
            .map { auditLogEntry -> auditLogEntry.responsibleUserId }
            .firstOrNull()

        val result = banAuthorId?.let { event.client.getUserById(banAuthorId).await() }

        return logger.exit(result)
    }
}
