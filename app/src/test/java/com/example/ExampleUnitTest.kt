package com.example

import com.example.data.BrowserPreferencesState
import com.example.data.ThemeMode
import com.example.engine.NewsCategory
import com.example.engine.NewsFeedService
import com.example.engine.SearchSuggestionService
import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests for browser preferences, OpenSearch suggestions, and live RSS news parsing.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun defaultPreferences_useAmoledBlackAndLiveSuggestions() {
    val defaults = BrowserPreferencesState()
    assertEquals(ThemeMode.AMOLED, defaults.themeMode)
    assertTrue(defaults.searchSuggestionsEnabled)
  }

  @Test
  fun searchSuggestionService_matchesActiveEngineAndSkipsUrls() {
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "google").contains("suggestqueries.google.com"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "google").contains("kotlin%20compose"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "duckduckgo").contains("duckduckgo.com/ac"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "bing").contains("api.bing.com/osjson.aspx"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "brave").contains("search.brave.com/api/suggest"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "wikipedia").contains("action=opensearch"))
    assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "custom_engine").contains("duckduckgo.com/ac"))

    assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("a"))
    assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("https://example.com"))
    assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("github.com"))
    assertTrue(SearchSuggestionService.shouldFetchRemoteSuggestions("android browser"))
  }

  @Test
  fun newsFeedService_parsesRssItemsImagesAndRelativeTime() {
    val sampleRss = """
      <?xml version="1.0" encoding="UTF-8"?>
      <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
        <channel>
          <item>
            <title>Major Discovery in Quantum Computing - Tech Daily</title>
            <link>https://example.com/quantum-news</link>
            <description>&lt;p&gt;Researchers achieved a new milestone &amp;amp; published results.&lt;/p&gt;</description>
            <source>Tech Daily</source>
            <pubDate>Tue, 24 Nov 2026 18:00:00 GMT</pubDate>
            <media:thumbnail url="https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/photo.jpg" />
          </item>
          <item>
            <title>Invalid Javascript Link</title>
            <link>javascript:alert(1)</link>
          </item>
        </channel>
      </rss>
    """.trimIndent()

    val parsed = NewsFeedService.parseRssXml(sampleRss, NewsCategory.TECH)
    assertEquals(1, parsed.size)
    val article = parsed.first()
    assertEquals("Major Discovery in Quantum Computing", article.title)
    assertEquals("Tech Daily", article.source)
    assertEquals("Researchers achieved a new milestone & published results.", article.summary)
    assertEquals("https://ichef.bbci.co.uk/ace/standard/800/cpsprodpb/photo.jpg", article.imageUrl)
    assertTrue(article.publishedAtMillis > 0L)

    val now = article.publishedAtMillis + 90L * 60_000L
    assertEquals("1h ago", NewsFeedService.formatRelativeTime(article.publishedAtMillis, now))
  }
}
