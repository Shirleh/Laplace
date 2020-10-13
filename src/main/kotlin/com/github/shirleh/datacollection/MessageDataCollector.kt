package com.github.shirleh.datacollection

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object MessageDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val channelRepository: ChannelRepository by inject()
    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects message data from the incoming [events].
     */
    suspend fun collect(events: Flow<MessageCreateEvent>) {
        events
            .filter { event ->
                val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return@filter false
                val channelId = event.message.channelId.asLong()
                channelRepository.findAll(guildId).contains(channelId)
            }
            .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
            .mapNotNull(::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toDataPoint(event: MessageCreateEvent): Point? {
        logger.entry(event)

        val message = event.message

        val channelId = message.channelId.asString()
        val authorId = message.author.map { it.id.asString() }.orElseNull() ?: return logger.exit(null)
        val contentLength = message.content.let { if (it.isBlank()) 0 else it.length }
        val timestamp = message.timestamp

        val result = Point.measurement("message")
            .addTag("channel", channelId)
            .addTag("author", authorId)
            .addField("length", contentLength)
            .time(timestamp, WritePrecision.S)

        return logger.exit(result)
    }
}
