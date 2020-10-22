package com.github.shirleh.statistics

import com.github.shirleh.persistence.influx.DataPointRepository
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

private data class BanData(val author: String, val count: Long = 1L) {
    fun toDataPoint() = Point.measurement("ban")
        .addTag("author", author)
        .addField("count", 1)
        .time(Instant.now(), WritePrecision.S)
}

object BanDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects ban data from the incoming [events].
     */
    fun addListener(events: Flow<BanEvent>) = events
        .buffer()
        .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
        .mapNotNull(BanDataCollector::findBanAuthorId)
        .map(BanDataCollector::toDataPoint)
        .onEach { logger.debug { it } }
        .map(BanData::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

    private suspend fun findBanAuthorId(event: BanEvent): Snowflake? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD) }
            .asFlow()
            .take(10)
            .filter { auditLogEntry -> auditLogEntry.targetId.orElse(null) == event.user.id }
            .map { auditLogEntry -> auditLogEntry.responsibleUserId }
            .firstOrNull()

        return logger.exit(result)
    }

    private fun toDataPoint(banAuthorId: Snowflake): BanData {
        logger.entry(banAuthorId)

        val result = BanData(banAuthorId.asString())

        return logger.exit(result)
    }
}
