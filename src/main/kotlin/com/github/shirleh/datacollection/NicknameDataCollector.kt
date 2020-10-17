package com.github.shirleh.datacollection

import com.github.shirleh.extensions.orElseNull
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

private data class NicknameChange(val author: String, val oldNickname: String, val newNickname: String) {
    fun toDataPoint() = Point.measurement("nickname")
        .addTag("author", author)
        .addField("oldNickname", oldNickname)
        .addField("currentNickname", newNickname)
        .time(Instant.now(), WritePrecision.S)
}

object NicknameDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects member nickname data from the incoming [events].
     */
    suspend fun collect(events: Flow<MemberUpdateEvent>) =
        events
            .onEach { delay(AUDIT_LOG_UPDATE_DELAY) }
            .mapNotNull(::findNicknameChange)
            .map(NicknameChange::toDataPoint)
            .collect(dataPointRepository::save)

    private suspend fun findNicknameChange(event: MemberUpdateEvent): NicknameChange? {
        logger.entry(event)

        val result = event.guild.awaitSingle()
            .getAuditLog { spec -> spec.setActionType(ActionType.MEMBER_UPDATE) }
            .asFlow()
            .mapNotNull { auditLogEntry ->
                auditLogEntry
                    .getChange(ChangeKey.USER_NICK)
                    .map {
                        NicknameChange(
                            author = auditLogEntry.responsibleUserId.asString(),
                            oldNickname = it.oldValue.orElse(""),
                            newNickname = it.currentValue.orElse("")
                        )
                    }
                    .orElseNull()
            }
            .filter { change -> event.currentNickname.map { it == change.newNickname }.orElse(false) }
            .firstOrNull()

        return logger.exit(result)
    }
}
