package com.github.shirleh.statistics.privacy

import com.github.shirleh.administration.Guilds
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

internal enum class Measurement(protected val s: String) {
    MESSAGE("message"),
    EMOJI("emoji"),
    VOICE("voice"),
    NICKNAME("nickname"),
    MEMBERSHIP("membership");

    fun asOption() = Pair(s, this)
}

data class PrivacySettings(
    val userId: Long,
    val guildId: Long,
    val message: Boolean = false,
    val emoji: Boolean = false,
    val voice: Boolean = false,
    val nickname: Boolean = false,
    val membership: Boolean = false,
) {
    val values = StringBuilder()
        .appendLine(message)
        .appendLine(emoji)
        .appendLine(voice)
        .appendLine(nickname)
        .appendLine(membership)
        .toString()

    internal fun optIn(options: List<Measurement>): PrivacySettings = options.fold(this) { result, option ->
        when (option) {
            Measurement.MESSAGE -> result.copy(message = true)
            Measurement.EMOJI -> result.copy(emoji = true)
            Measurement.VOICE -> result.copy(voice = true)
            Measurement.NICKNAME -> result.copy(nickname = true)
            Measurement.MEMBERSHIP -> result.copy(membership = true)
        }
    }

    internal fun optOut(options: List<Measurement>): PrivacySettings = options.fold(this) { result, option ->
        when (option) {
            Measurement.MESSAGE -> result.copy(message = false)
            Measurement.EMOJI -> result.copy(emoji = false)
            Measurement.VOICE -> result.copy(voice = false)
            Measurement.NICKNAME -> result.copy(nickname = false)
            Measurement.MEMBERSHIP -> result.copy(membership = false)
        }
    }
}

internal class PrivacySettingsEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PrivacySettingsEntity>(PrivacySettingsTable)

    var userId by PrivacySettingsTable.user
    var guildId by PrivacySettingsTable.guild

    var message by PrivacySettingsTable.message
    var emoji by PrivacySettingsTable.emoji
    var voice by PrivacySettingsTable.voice
    var nickname by PrivacySettingsTable.nickname
    var membership by PrivacySettingsTable.membership
}

internal object PrivacySettingsTable : LongIdTable(name = "opt_in_settings") {
    val user = reference("user_id", Users.id)
    val guild = reference("guild_id", Guilds.id)
    val message = bool("message").default(false)
    val emoji = bool("emoji").default(false)
    val voice = bool("voice").default(false)
    val nickname = bool("nickname").default(false)
    val membership = bool("membership").default(false)
}
