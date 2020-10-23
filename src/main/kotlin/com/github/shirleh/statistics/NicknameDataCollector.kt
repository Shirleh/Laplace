package com.github.shirleh.statistics

import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
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
import java.time.Instant

private data class NicknameChange(
    val author: String,
    val newNickname: String,
    val hadNickname: Boolean,
) {
    fun toDataPoint() = Point.measurement("nickname")
        .addTag("author", author)
        .addField("nickname", newNickname)
        .addField("hadNickname", hadNickname)
        .time(Instant.now(), WritePrecision.MS)
}

object NicknameDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects member nickname data from the incoming [events].
     */
    fun addListener(events: Flow<MemberUpdateEvent>) = events
        .buffer()
        .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
        .mapNotNull(::findNicknameChange)
        .onEach { logger.debug { it } }
        .map(NicknameChange::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }


    private suspend fun findNicknameChange(event: MemberUpdateEvent): NicknameChange? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_UPDATE) }
            .asFlow()
            .take(10)
            .mapNotNull { auditLogEntry ->
                auditLogEntry
                    .getChange(ChangeKey.USER_NICK)
                    .map {
                        NicknameChange(
                            author = auditLogEntry.responsibleUserId.asString(),
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
