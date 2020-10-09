package com.github.shirleh.datacollection

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.`object`.audit.ActionType
import discord4j.core.event.domain.guild.BanEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

object BanDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects ban data from the incoming [events].
     */
    suspend fun collect(events: Flow<BanEvent>) =
        events
            .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
            .mapNotNull(::findBanAuthorId)
            .map(::toDataPoint)
            .collect(dataPointRepository::save)

    private suspend fun findBanAuthorId(event: BanEvent): Snowflake? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD) }
            .asFlow()
            .filter { auditLogEntry -> auditLogEntry.targetId.orElse(null) == event.user.id }
            .map { auditLogEntry -> auditLogEntry.responsibleUserId }
            .firstOrNull()

        return logger.exit(result)
    }

    private fun toDataPoint(banAuthorId: Snowflake): Point {
        logger.entry(banAuthorId)

        val result = Point.measurement("bans")
            .addTag("author", banAuthorId.asString())
            .addField("count", 1)
            .time(Instant.now(), WritePrecision.S)

        return logger.exit(result)
    }
}
