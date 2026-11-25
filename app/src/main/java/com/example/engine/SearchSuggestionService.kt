package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fetches real-time search autocomplete suggestions via public, zero-key OpenSearch APIs
 * matched to the currently selected search engine (Google, DuckDuckGo, Bing, Brave, Wikipedia).
 */
class SearchSuggestionService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .callTimeout(3000, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun fetchSuggestions(
        query: String,
        engineId: String,
        maxResults: Int = 6
    ): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (!shouldFetchRemoteSuggestions(trimmed)) return@withContext emptyList()

        val endpoint = buildSuggestionUrl(trimmed, engineId)
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json, text/javascript, */*")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string().orEmpty()
                parseSuggestionsJson(body, trimmed, maxResults)
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        fun shouldFetchRemoteSuggestions(query: String): Boolean {
            val trimmed = query.trim()
            if (trimmed.length < 2) return false
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("about:", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true)
            ) {
                return false
            }
            // Skip likely direct domains/URLs (no spaces and contains a dot with valid TLD shape)
            if (!trimmed.contains(" ") && trimmed.contains(".")) {
                val parts = trimmed.split(".")
                if (parts.size >= 2 && parts.last().length in 2..12 && parts.last().all { it.isLetter() }) {
                    return false
                }
            }
            return true
        }

        fun buildSuggestionUrl(query: String, engineId: String): String {
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name()).replace("+", "%20")
            return when (engineId.lowercase()) {
                "google" -> "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
                "duckduckgo" -> "https://duckduckgo.com/ac/?q=$encoded&type=list"
                "bing" -> "https://api.bing.com/osjson.aspx?query=$encoded"
                "brave" -> "https://search.brave.com/api/suggest?q=$encoded"
                "wikipedia" -> "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=8&namespace=0&format=json"
                else -> "https://duckduckgo.com/ac/?q=$encoded&type=list"
            }
        }

        /**
         * Parses both standard OpenSearch array responses:
         *   ["kotlin", ["kotlin coroutines", "kotlin compose", ...]]
         * and DuckDuckGo object array responses:
         *   [{"phrase": "kotlin coroutines"}, ...]
         */
        fun parseSuggestionsJson(
            rawJson: String,
            originalQuery: String,
            maxResults: Int = 6
        ): List<String> {
            val trimmedBody = rawJson.trim()
            if (trimmedBody.isEmpty() || !trimmedBody.startsWith("[")) return emptyList()

            val normalizedQuery = originalQuery.trim().lowercase()
            val results = LinkedHashSet<String>()

            runCatching {
                val root = JSONArray(trimmedBody)
                if (root.length() == 0) return emptyList()

                // Format 1: Standard OpenSearch [query, [suggestions...], ...]
                if (root.length() >= 2 && root.optJSONArray(1) != null) {
                    val suggestionsArr = root.getJSONArray(1)
                    for (i in 0 until suggestionsArr.length()) {
                        val term = suggestionsArr.optString(i).trim()
                        if (term.isNotEmpty() && term.lowercase() != normalizedQuery) {
                            results.add(term)
                            if (results.size >= maxResults) break
                        }
                    }
                    return results.toList()
                }

                // Format 2: DuckDuckGo object list [{"phrase": "..."}, ...]
                for (i in 0 until root.length()) {
                    val obj = root.optJSONObject(i) ?: continue
                    val phrase = obj.optString("phrase").trim()
                    if (phrase.isNotEmpty() && phrase.lowercase() != normalizedQuery) {
                        results.add(phrase)
                        if (results.size >= maxResults) break
                    }
                }
            }

            return results.toList()
        }
    }
}
