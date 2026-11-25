package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

enum class NewsCategory(
    val label: String,
    val primaryFeedUrl: String,
    val fallbackFeedUrl: String,
    val defaultSourceName: String
) {
    TOP_STORIES(
        label = "Top Stories",
        primaryFeedUrl = "https://feeds.bbci.co.uk/news/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC News"
    ),
    TECH(
        label = "Tech",
        primaryFeedUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC Tech"
    ),
    WORLD(
        label = "World",
        primaryFeedUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC World"
    ),
    BUSINESS(
        label = "Business",
        primaryFeedUrl = "https://feeds.bbci.co.uk/news/business/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC Business"
    ),
    SCIENCE(
        label = "Science",
        primaryFeedUrl = "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC Science"
    ),
    SPORTS(
        label = "Sports",
        primaryFeedUrl = "https://feeds.bbci.co.uk/sport/rss.xml",
        fallbackFeedUrl = "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=en-US&gl=US&ceid=US:en",
        defaultSourceName = "BBC Sport"
    )
}

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val imageUrl: String?,
    val publishedAtMillis: Long,
    val category: NewsCategory
)

/**
 * Fetches real, live news headlines via public zero-key RSS feeds (BBC News + Google News fallback)
 * with secure XML parsing and a 5-minute in-memory cache per category.
 */
class NewsFeedService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5000, TimeUnit.MILLISECONDS)
        .readTimeout(6000, TimeUnit.MILLISECONDS)
        .callTimeout(8000, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()
) {
    private data class CachedFeed(
        val fetchedAtMillis: Long,
        val articles: List<NewsArticle>
    )

    private val cache = ConcurrentHashMap<NewsCategory, CachedFeed>()

    suspend fun fetchArticles(
        category: NewsCategory,
        forceRefresh: Boolean = false,
        maxItems: Int = 18
    ): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = cache[category]
            if (cached != null && (now - cached.fetchedAtMillis) < CACHE_TTL_MS && cached.articles.isNotEmpty()) {
                return@withContext Result.success(cached.articles)
            }
        }

        val primaryResult = fetchFromUrl(category.primaryFeedUrl, category, category.defaultSourceName, maxItems)
        if (primaryResult.isNotEmpty()) {
            cache[category] = CachedFeed(now, primaryResult)
            return@withContext Result.success(primaryResult)
        }

        val fallbackResult = fetchFromUrl(category.fallbackFeedUrl, category, "Google News", maxItems)
        if (fallbackResult.isNotEmpty()) {
            cache[category] = CachedFeed(now, fallbackResult)
            return@withContext Result.success(fallbackResult)
        }

        // Return stale cache if offline and previously loaded
        val stale = cache[category]?.articles
        if (!stale.isNullOrEmpty()) {
            return@withContext Result.success(stale)
        }

        Result.failure(IllegalStateException("Unable to load news headlines right now."))
    }

    private fun fetchFromUrl(
        feedUrl: String,
        category: NewsCategory,
        defaultSource: String,
        maxItems: Int
    ): List<NewsArticle> {
        val request = Request.Builder()
            .url(feedUrl)
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val xml = response.body?.string().orEmpty()
                parseRssXml(xml, category, defaultSource, maxItems)
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val CACHE_TTL_MS = 5L * 60L * 1000L

        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val IMG_SRC_REGEX = Regex("""<img[^>]+src=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val WHITESPACE_REGEX = Regex("\\s+")

        fun parseRssXml(
            rawXml: String,
            category: NewsCategory,
            defaultSource: String = category.defaultSourceName,
            maxItems: Int = 18
        ): List<NewsArticle> {
            val trimmed = rawXml.trim()
            if (trimmed.isEmpty() || !trimmed.startsWith("<")) return emptyList()

            return runCatching {
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                    runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                    runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                    runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                    runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                    isExpandEntityReferences = false
                }
                val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(trimmed)))
                val items = doc.getElementsByTagName("item")
                val results = mutableListOf<NewsArticle>()
                val seenUrls = HashSet<String>()

                for (i in 0 until items.length) {
                    if (results.size >= maxItems) break
                    val node = items.item(i) as? Element ?: continue

                    val rawTitle = node.firstChildText("title").orEmpty().trim()
                    val link = node.firstChildText("link").orEmpty().trim()
                    if (rawTitle.isBlank() || (!link.startsWith("http://") && !link.startsWith("https://"))) {
                        continue
                    }
                    if (!seenUrls.add(link)) continue

                    val rawDesc = node.firstChildText("description").orEmpty()
                    val cleanSummary = stripHtml(rawDesc)
                    val explicitSource = node.firstChildText("source")?.trim().orEmpty()

                    // Google News titles often end with " - Publisher Name"
                    val (cleanTitle, parsedSource) = extractTitleAndSource(rawTitle, explicitSource, defaultSource)

                    val imageUrl = extractImageUrl(node, rawDesc)
                    val pubDateStr = node.firstChildText("pubDate").orEmpty().trim()
                    val pubMillis = parseRssDate(pubDateStr)

                    results.add(
                        NewsArticle(
                            id = "${category.name}_${link.hashCode()}",
                            title = cleanTitle,
                            summary = if (cleanSummary.equals(cleanTitle, ignoreCase = true)) "" else cleanSummary,
                            url = link,
                            source = parsedSource,
                            imageUrl = imageUrl,
                            publishedAtMillis = pubMillis,
                            category = category
                        )
                    )
                }
                results
            }.getOrDefault(emptyList())
        }

        private fun Element.firstChildText(tagName: String): String? {
            val list = getElementsByTagName(tagName)
            if (list.length == 0) return null
            return list.item(0)?.textContent
        }

        private fun extractImageUrl(item: Element, rawDescription: String): String? {
            // 1. <media:thumbnail url="..." /> or <media:content url="..." />
            val mediaTags = listOf("media:thumbnail", "thumbnail", "media:content", "content")
            for (tag in mediaTags) {
                val nodes = item.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val el = nodes.item(i) as? Element ?: continue
                    val candidate = el.getAttribute("url").trim()
                    if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
                        // Upgrade BBC small thumbnail path to crisp 800w when present
                        return candidate
                            .replace("/240/", "/800/")
                            .replace("/320/", "/800/")
                    }
                }
            }
            // 2. <enclosure url="..." type="image/..." />
            val enclosures = item.getElementsByTagName("enclosure")
            for (i in 0 until enclosures.length) {
                val el = enclosures.item(i) as? Element ?: continue
                val url = el.getAttribute("url").trim()
                val type = el.getAttribute("type").lowercase()
                if ((url.startsWith("https://") || url.startsWith("http://")) &&
                    (type.isBlank() || type.startsWith("image/"))
                ) {
                    return url
                }
            }
            // 3. Inline <img src="..."> inside description
            val imgMatch = IMG_SRC_REGEX.find(rawDescription)
            return imgMatch?.groupValues?.getOrNull(1)
        }

        private fun extractTitleAndSource(
            rawTitle: String,
            explicitSource: String,
            defaultSource: String
        ): Pair<String, String> {
            val unescaped = unescapeBasicHtml(rawTitle).trim()
            if (explicitSource.isNotBlank()) {
                val suffix = " - $explicitSource"
                val cleaned = if (unescaped.endsWith(suffix, ignoreCase = true)) {
                    unescaped.dropLast(suffix.length).trim()
                } else {
                    unescaped
                }
                return cleaned to explicitSource
            }
            return unescaped to defaultSource
        }

        private fun stripHtml(html: String): String {
            if (html.isBlank()) return ""
            val noTags = HTML_TAG_REGEX.replace(html, " ")
            return unescapeBasicHtml(noTags)
                .replace(WHITESPACE_REGEX, " ")
                .trim()
                .take(220)
        }

        private fun unescapeBasicHtml(text: String): String {
            return text
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
        }

        fun parseRssDate(dateStr: String): Long {
            if (dateStr.isBlank()) return 0L
            val patterns = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (pattern in patterns) {
                val parsed = runCatching {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("GMT")
                    sdf.parse(dateStr)?.time
                }.getOrNull()
                if (parsed != null && parsed > 0L) return parsed
            }
            return 0L
        }

        fun formatRelativeTime(publishedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
            if (publishedAtMillis <= 0L) return "Latest"
            val diffMinutes = ((nowMillis - publishedAtMillis).coerceAtLeast(0L)) / 60_000L
            return when {
                diffMinutes < 2L -> "Just now"
                diffMinutes < 60L -> "${diffMinutes}m ago"
                diffMinutes < 24L * 60L -> "${diffMinutes / 60L}h ago"
                diffMinutes < 48L * 60L -> "Yesterday"
                else -> "${diffMinutes / (24L * 60L)}d ago"
            }
        }
    }
}
