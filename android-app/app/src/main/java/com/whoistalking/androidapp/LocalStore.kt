package com.whoistalking.androidapp

import android.app.Application
import java.util.UUID

class LocalStore(app: Application) {
    private val prefs = app.getSharedPreferences("who_is_talking", 0)

    fun getOrCreateClientId(): String {
        val current = prefs.getString("client_id", null)
        if (current != null) return current
        val created = UUID.randomUUID().toString()
        prefs.edit().putString("client_id", created).apply()
        return created
    }

    fun getLastSeenMessageId(sessionId: String): String? = prefs.getString("last_seen_$sessionId", null)

    fun setLastSeenMessageId(sessionId: String, messageId: String) {
        prefs.edit().putString("last_seen_$sessionId", messageId).apply()
    }

    fun getLastSequenceId(sessionId: String): Int? {
        val raw = prefs.getInt("last_sequence_$sessionId", -1)
        return if (raw >= 0) raw else null
    }

    fun setLastSequenceId(sessionId: String, sequenceId: Int) {
        prefs.edit().putInt("last_sequence_$sessionId", sequenceId).apply()
    }

}
