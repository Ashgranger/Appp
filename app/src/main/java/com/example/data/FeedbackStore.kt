package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FeedbackEntry(
    val id: String = UUID.randomUUID().toString(),
    val rating: Int,
    val category: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class FeedbackStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _entriesFlow = MutableStateFlow<List<FeedbackEntry>>(load())
    val entriesFlow: StateFlow<List<FeedbackEntry>> = _entriesFlow.asStateFlow()

    @Synchronized
    fun submit(rating: Int, category: String, message: String): FeedbackEntry {
        val entry = FeedbackEntry(
            rating = rating.coerceIn(1, 5),
            category = category.ifBlank { "General" },
            message = message.trim()
        )
        val updated = (listOf(entry) + _entriesFlow.value).take(50)
        persist(updated)
        _entriesFlow.value = updated
        return entry
    }

    private fun load(): List<FeedbackEntry> {
        val raw = prefs.getString(KEY_FEEDBACK_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                FeedbackEntry(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    rating = obj.optInt("rating", 5),
                    category = obj.optString("category", "General"),
                    message = obj.optString("message", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(entries: List<FeedbackEntry>) {
        val arr = JSONArray()
        for (item in entries) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("rating", item.rating)
                    put("category", item.category)
                    put("message", item.message)
                    put("timestamp", item.timestamp)
                }
            )
        }
        prefs.edit().putString(KEY_FEEDBACK_JSON, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "swiftweb_feedback_local"
        private const val KEY_FEEDBACK_JSON = "feedback_entries_v1"
    }
}
