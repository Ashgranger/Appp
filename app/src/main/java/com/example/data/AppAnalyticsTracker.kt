package com.example.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyActivityPoint(
    val dateKey: String,
    val dayLabel: String,
    val sessions: Int
)

data class AppUsageMetrics(
    val firstInstalledAt: Long = 0L,
    val totalSessions: Int = 0,
    val activeDaysCount: Int = 0,
    val currentStreakDays: Int = 0,
    val last7Days: List<DailyActivityPoint> = emptyList(),
    val totalPagesVisited: Int = 0,
    val totalDownloadsStarted: Int = 0,
    val apkDownloadsStarted: Int = 0,
    val passwordsSavedCount: Int = 0,
    val autofillUsesCount: Int = 0,
    val firebaseAnalyticsActive: Boolean = false,
    val firebaseProjectId: String = DEFAULT_FIREBASE_PROJECT_ID
) {
    companion object {
        const val DEFAULT_FIREBASE_PROJECT_ID = "gen-lang-client-0651876307"
    }
}

/**
 * Tracks privacy-safe browser app usage & downloads:
 * - Global installs (`first_open`) and Daily Active Users (`session_start` / `browser_daily_active`)
 *   via Firebase Analytics with zero sign-in required.
 * - Local on-device stats (daily sessions, streak, 7-day chart, APK/file downloads, vault activity)
 *   for the in-app Usage & App Insights screen.
 * - Strictly excludes Incognito browsing and never logs URLs, hostnames, filenames, or credentials.
 */
class AppAnalyticsTracker(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val firebaseAnalytics: FirebaseAnalytics? = runCatching {
        FirebaseAnalytics.getInstance(appContext)
    }.getOrNull()

    private val resolvedProjectId: String = runCatching {
        FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: AppUsageMetrics.DEFAULT_FIREBASE_PROJECT_ID

    private val _metricsFlow = MutableStateFlow(loadMetrics(System.currentTimeMillis()))
    val metricsFlow: StateFlow<AppUsageMetrics> = _metricsFlow.asStateFlow()

    @Synchronized
    fun recordAppOpen(nowMillis: Long = System.currentTimeMillis()) {
        val firstInstalled = prefs.getLong(KEY_FIRST_INSTALLED_AT, 0L).let { existing ->
            if (existing > 0L) existing else nowMillis
        }
        val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1
        val todayKey = formatDateKey(nowMillis)

        val dailyMap = readDailyCounts().toMutableMap()
        dailyMap[todayKey] = (dailyMap[todayKey] ?: 0) + 1
        val prunedMap = pruneOldDays(dailyMap, maxDaysToKeep = 90)

        val lastDauLogged = prefs.getString(KEY_LAST_DAU_DATE, "")
        val shouldLogDailyActive = lastDauLogged != todayKey

        prefs.edit()
            .putLong(KEY_FIRST_INSTALLED_AT, firstInstalled)
            .putInt(KEY_TOTAL_SESSIONS, totalSessions)
            .putString(KEY_DAILY_COUNTS_JSON, serializeDailyCounts(prunedMap))
            .putString(KEY_LAST_DAU_DATE, todayKey)
            .apply()

        logFirebaseEvent(FirebaseAnalytics.Event.APP_OPEN)
        if (shouldLogDailyActive) {
            logFirebaseEvent(EVENT_DAILY_ACTIVE)
        }

        _metricsFlow.value = loadMetrics(nowMillis)
    }

    @Synchronized
    fun recordPageVisit(isIncognito: Boolean) {
        if (isIncognito) return
        val next = prefs.getInt(KEY_PAGES_VISITED, 0) + 1
        prefs.edit().putInt(KEY_PAGES_VISITED, next).apply()
        _metricsFlow.value = loadMetrics(System.currentTimeMillis())
    }

    @Synchronized
    fun recordDownloadStarted(fileName: String, mimeType: String, isApk: Boolean) {
        val nextTotal = prefs.getInt(KEY_DOWNLOADS_STARTED, 0) + 1
        val nextApk = prefs.getInt(KEY_APK_DOWNLOADS_STARTED, 0) + if (isApk) 1 else 0
        prefs.edit()
            .putInt(KEY_DOWNLOADS_STARTED, nextTotal)
            .putInt(KEY_APK_DOWNLOADS_STARTED, nextApk)
            .apply()

        val params = Bundle().apply {
            putLong("is_apk", if (isApk) 1L else 0L)
        }
        logFirebaseEvent(EVENT_BROWSER_DOWNLOAD, params)
        _metricsFlow.value = loadMetrics(System.currentTimeMillis())
    }

    @Synchronized
    fun recordPasswordSaved() {
        val next = prefs.getInt(KEY_PASSWORDS_SAVED, 0) + 1
        prefs.edit().putInt(KEY_PASSWORDS_SAVED, next).apply()
        logFirebaseEvent(EVENT_PASSWORD_SAVED)
        _metricsFlow.value = loadMetrics(System.currentTimeMillis())
    }

    @Synchronized
    fun recordPasswordAutofilled() {
        val next = prefs.getInt(KEY_AUTOFILL_USES, 0) + 1
        prefs.edit().putInt(KEY_AUTOFILL_USES, next).apply()
        logFirebaseEvent(EVENT_PASSWORD_AUTOFILLED)
        _metricsFlow.value = loadMetrics(System.currentTimeMillis())
    }

    @Synchronized
    fun resetLocalStats(nowMillis: Long = System.currentTimeMillis()) {
        val preservedInstall = prefs.getLong(KEY_FIRST_INSTALLED_AT, 0L).let {
            if (it > 0L) it else nowMillis
        }
        prefs.edit()
            .clear()
            .putLong(KEY_FIRST_INSTALLED_AT, preservedInstall)
            .apply()
        _metricsFlow.value = loadMetrics(nowMillis)
    }

    private fun logFirebaseEvent(name: String, params: Bundle? = null) {
        runCatching {
            firebaseAnalytics?.logEvent(name, params)
        }
    }

    private fun loadMetrics(nowMillis: Long): AppUsageMetrics {
        val firstInstalled = prefs.getLong(KEY_FIRST_INSTALLED_AT, 0L)
        val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        val dailyMap = readDailyCounts()
        val activeDays = dailyMap.count { it.value > 0 }
        val streak = computeStreakDays(dailyMap, nowMillis)
        val last7 = buildLast7Days(dailyMap, nowMillis)

        return AppUsageMetrics(
            firstInstalledAt = firstInstalled,
            totalSessions = totalSessions,
            activeDaysCount = activeDays,
            currentStreakDays = streak,
            last7Days = last7,
            totalPagesVisited = prefs.getInt(KEY_PAGES_VISITED, 0),
            totalDownloadsStarted = prefs.getInt(KEY_DOWNLOADS_STARTED, 0),
            apkDownloadsStarted = prefs.getInt(KEY_APK_DOWNLOADS_STARTED, 0),
            passwordsSavedCount = prefs.getInt(KEY_PASSWORDS_SAVED, 0),
            autofillUsesCount = prefs.getInt(KEY_AUTOFILL_USES, 0),
            firebaseAnalyticsActive = firebaseAnalytics != null,
            firebaseProjectId = resolvedProjectId
        )
    }

    private fun readDailyCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_DAILY_COUNTS_JSON, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = LinkedHashMap<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val count = obj.optInt(key, 0)
                if (count > 0) result[key] = count
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializeDailyCounts(map: Map<String, Int>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> if (v > 0) obj.put(k, v) }
        return obj.toString()
    }

    private fun pruneOldDays(map: Map<String, Int>, maxDaysToKeep: Int): Map<String, Int> {
        if (map.size <= maxDaysToKeep) return map
        return map.entries
            .sortedByDescending { it.key }
            .take(maxDaysToKeep)
            .associate { it.key to it.value }
    }

    private fun computeStreakDays(counts: Map<String, Int>, nowMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayKey = formatDateKey(cal.timeInMillis)
        if ((counts[todayKey] ?: 0) <= 0) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        var streak = 0
        while (streak < 365) {
            val key = formatDateKey(cal.timeInMillis)
            if ((counts[key] ?: 0) > 0) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun buildLast7Days(counts: Map<String, Int>, nowMillis: Long): List<DailyActivityPoint> {
        val dayLabelFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableListOf<DailyActivityPoint>()
        for (offset in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val key = formatDateKey(cal.timeInMillis)
            val label = dayLabelFmt.format(Date(cal.timeInMillis))
            result.add(
                DailyActivityPoint(
                    dateKey = key,
                    dayLabel = label,
                    sessions = counts[key] ?: 0
                )
            )
        }
        return result
    }

    private fun formatDateKey(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(millis))
    }

    companion object {
        private const val PREFS_NAME = "browser_usage_analytics_v1"
        private const val KEY_FIRST_INSTALLED_AT = "first_installed_at"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_DAILY_COUNTS_JSON = "daily_counts_json"
        private const val KEY_LAST_DAU_DATE = "last_dau_date"
        private const val KEY_PAGES_VISITED = "pages_visited"
        private const val KEY_DOWNLOADS_STARTED = "downloads_started"
        private const val KEY_APK_DOWNLOADS_STARTED = "apk_downloads_started"
        private const val KEY_PASSWORDS_SAVED = "passwords_saved"
        private const val KEY_AUTOFILL_USES = "autofill_uses"

        private const val EVENT_DAILY_ACTIVE = "browser_daily_active"
        private const val EVENT_BROWSER_DOWNLOAD = "browser_download"
        private const val EVENT_PASSWORD_SAVED = "password_saved"
        private const val EVENT_PASSWORD_AUTOFILLED = "password_autofilled"
    }
}
