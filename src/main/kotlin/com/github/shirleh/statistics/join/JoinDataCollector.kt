package com.github.shirleh.statistics.join

import discord4j.core.event.domain.guild.MemberJoinEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object JoinDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val joinPointRepository: JoinPointRepository by inject()

    /**
     * Collects member join data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
    fun addListener(events: Flow<MemberJoinEvent>) = events
        .filter { event -> !event.member.isBot }
        .buffer()
        .flatMapConcat { event ->
            flowOf(event)
                .map(this::aggregateJoinData)
                .onEach(joinPointRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private fun aggregateJoinData(event: MemberJoinEvent): JoinPoint {
        logger.entry(event)

        val member = event.member

        val result = JoinPoint(
            guildId = event.guildId.asString(),
            creationDate = member.id.timestamp,
            isBoosting = member.premiumTime.isPresent,
            joinTime = member.joinTime
        )

        return logger.exit(result)
    }
}
