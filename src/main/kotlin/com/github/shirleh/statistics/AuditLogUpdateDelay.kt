package com.github.shirleh.statistics

/**
 * Arbitrary delay to prevent audit log access before it's updated.
 *
 * In Discord, there's a delay between dispatching an event and updating the audit log with said event.
 * Larger guilds tend to have longer delays. 5 seconds is commonly used which works *most of the time*.
 */
const val AUDIT_LOG_UPDATE_DELAY = 5000L
