package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BookmarkEntity
import com.example.data.BrowserPreferencesState
import com.example.engine.NewsArticle
import com.example.engine.NewsCategory
import com.example.ui.components.CompactNewsCard
import com.example.ui.components.FavoriteSiteIcon
import com.example.ui.components.FeaturedNewsCard
import com.example.ui.components.NewsCategoryStrip
import com.example.ui.components.NewsErrorCard
import com.example.ui.components.NewsLoadingPlaceholder
import com.example.ui.components.NewsSectionHeader
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.viewmodel.MostVisitedSite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartPageView(
    isIncognito: Boolean,
    preferences: BrowserPreferencesState,
    activeSearchEngineName: String,
    mostVisitedSites: List<MostVisitedSite>,
    speedDials: List<BookmarkEntity>,
    selectedNewsCategory: NewsCategory,
    newsArticles: List<NewsArticle>,
    isLoadingNews: Boolean,
    newsError: String?,
    onOpenOmnibox: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onOpenInNewTab: (url: String, incognito: Boolean) -> Unit,
    onSwitchMode: (incognito: Boolean) -> Unit,
    onBurnSession: () -> Unit,
    onPinMostVisited: (MostVisitedSite) -> Unit,
    onDismissMostVisited: (String) -> Unit,
    onRestoreDismissedMostVisited: () -> Unit,
    onAddSpeedDial: (title: String, url: String) -> Unit,
    onDeleteSpeedDial: (Long) -> Unit,
    onSelectNewsCategory: (NewsCategory) -> Unit,
    onRefreshNews: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var selectedMostVisited by remember { mutableStateOf<MostVisitedSite?>(null) }
    var selectedArticleForMenu by remember { mutableStateOf<NewsArticle?>(null) }
    var showBurnConfirm by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val clipboardUrl = remember {
        clipboard.getText()?.text?.trim()?.takeIf {
            it.startsWith("http://", true) || it.startsWith("https://", true)
        }
    }

    val ambientPrimary = if (isIncognito) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary
    val ambientSecondary = if (isIncognito) Color(0xFF6366F1) else ShieldEmerald

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientPrimary.copy(alpha = if (isIncognito) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.04f),
                        radius = size.width * 0.78f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientSecondary.copy(alpha = if (isIncognito) 0.08f else 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.24f),
                        radius = size.width * 0.65f
                    )
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Minimalist Top Utility Bar: Mode Switcher + Burn + Settings
            item(key = "top_mode_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModePillItem(
                                icon = Icons.Rounded.Explore,
                                label = "Browse",
                                selected = !isIncognito,
                                onClick = { if (isIncognito) onSwitchMode(false) },
                                tag = "start_mode_standard"
                            )
                            ModePillItem(
                                icon = Icons.Rounded.VisibilityOff,
                                label = "Private",
                                selected = isIncognito,
                                onClick = { if (!isIncognito) onSwitchMode(true) },
                                tag = "start_mode_incognito"
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showBurnConfirm = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("start_burn_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Burn tabs and browsing data",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenSettings,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("start_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Browser settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Clean Floating Search Capsule (no banner logo or title)
            item(key = "start_search_capsule") {
                Surface(
                    onClick = onOpenOmnibox,
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ambientPrimary.copy(alpha = 0.42f),
                                MaterialTheme.colorScheme.outlineVariant,
                                ambientSecondary.copy(alpha = 0.30f)
                            )
                        )
                    ),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_compact_search_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ambientPrimary.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search or enter URL",
                                tint = ambientPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Search or enter website",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = activeSearchEngineName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Optional Clipboard Paste-and-Go Pill
            if (clipboardUrl != null) {
                item(key = "clipboard_quick_banner") {
                    Surface(
                        onClick = { onNavigateUrl(clipboardUrl) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_clipboard_go_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open Copied Link",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = clipboardUrl,
                                    style = MonoCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (!isIncognito) {
                // 3. Most Visited Sites Grid (auto-populated from browsing history)
                if (preferences.startPageShowMostVisited) {
                    item(key = "most_visited_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                        .border(
                                            width = 0.8.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Most Visited",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (mostVisitedSites.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                                    ) {
                                        Text(
                                            text = mostVisitedSites.size.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (preferences.dismissedMostVisitedHosts.isNotEmpty()) {
                                TextButton(
                                    onClick = onRestoreDismissedMostVisited,
                                    modifier = Modifier.testTag("restore_most_visited_button")
                                ) {
                                    Text("Restore Hidden")
                                }
                            } else if (mostVisitedSites.isNotEmpty()) {
                                Text(
                                    text = "Hold to pin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (mostVisitedSites.isEmpty()) {
                        item(key = "most_visited_empty") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("most_visited_empty_state")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Sites you visit frequently appear here automatically. Long-press any tile to pin or dismiss.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        val mvCols = 4
                        val mvRows = mostVisitedSites.chunked(mvCols)
                        items(mvRows.size, key = { "mv_row_$it" }) { rowIndex ->
                            val rowItems = mvRows[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { site ->
                                    MostVisitedTile(
                                        site = site,
                                        onClick = { onNavigateUrl(site.url) },
                                        onLongClick = { selectedMostVisited = site },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(mvCols - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 4. Pinned Favorites Grid Header
                item(key = "speed_dial_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF38BDF8).copy(alpha = 0.22f),
                                                ShieldEmerald.copy(alpha = 0.16f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = ShieldEmerald.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(9.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    tint = ShieldEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "Pinned Favorites",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (speedDials.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = speedDials.size.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("add_speed_dial_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add Pin",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // 4. Pinned Favorites Grid
                if (speedDials.isEmpty()) {
                    item(key = "speed_dial_empty") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("speed_dial_empty_state")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ShieldEmerald.copy(alpha = 0.14f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = ShieldEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "No Pinned Favorites Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pin your go-to websites here for instant one-tap launch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pin First Favorite")
                                }
                            }
                        }
                    }
                } else {
                    val cols = preferences.speedDialColumns.coerceIn(3, 5)
                    val gridEntries: List<BookmarkEntity?> = speedDials + listOf(null)
                    val rows = gridEntries.chunked(cols)

                    items(rows.size, key = { "sd_row_$it" }) { rowIndex ->
                        val rowItems = rows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { dial ->
                                if (dial != null) {
                                    SpeedDialTile(
                                        bookmark = dial,
                                        onClick = { onNavigateUrl(dial.url) },
                                        onLongClick = { editingBookmark = dial },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    AddFavoriteGridTile(
                                        onClick = { showAddDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            repeat(cols - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 5. Scroll-Down Live News Feed Section
                item(key = "news_section_header") {
                    NewsSectionHeader(
                        isLoading = isLoadingNews,
                        onRefresh = onRefreshNews,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item(key = "news_category_strip") {
                    NewsCategoryStrip(
                        selectedCategory = selectedNewsCategory,
                        onSelectCategory = onSelectNewsCategory
                    )
                }

                when {
                    isLoadingNews && newsArticles.isEmpty() -> {
                        item(key = "news_loading_skeleton") {
                            NewsLoadingPlaceholder()
                        }
                    }
                    newsError != null && newsArticles.isEmpty() -> {
                        item(key = "news_error_card") {
                            NewsErrorCard(
                                message = newsError,
                                onRetry = onRefreshNews
                            )
                        }
                    }
                    newsArticles.isNotEmpty() -> {
                        val featured = newsArticles.first()
                        val remaining = newsArticles.drop(1)

                        item(key = "news_featured_${featured.id}") {
                            FeaturedNewsCard(
                                article = featured,
                                onClick = { onNavigateUrl(featured.url) },
                                onLongClick = { selectedArticleForMenu = featured }
                            )
                        }

                        items(remaining, key = { it.id }) { article ->
                            CompactNewsCard(
                                article = article,
                                onClick = { onNavigateUrl(article.url) },
                                onLongClick = { selectedArticleForMenu = article }
                            )
                        }
                    }
                }
            } else {
                // Private Mode Assurance Card
                item(key = "incognito_details") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.35f)),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_incognito_shield_1795111523288),
                                    contentDescription = "Private browsing protection",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                                                )
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Private Browsing Mode",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "• Browsing history and live search suggestions stay paused.\n" +
                                        "• Cookies and site storage are isolated and wiped when your last private tab closes.\n" +
                                        "• Logins entered in Private mode are never prompted to save.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_breathing_space") {
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showAddDialog) {
        AddSpeedDialDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, url ->
                onAddSpeedDial(title, url)
                showAddDialog = false
            }
        )
    }

    if (showBurnConfirm) {
        AlertDialog(
            onDismissRequest = { showBurnConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Burn Tabs & Data?") },
            text = {
                Text("Closes all open tabs and permanently wipes browsing history, cookies, and web cache.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBurnConfirm = false
                        onBurnSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_burn_button")
                ) {
                    Text("Burn Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBurnConfirm = false }) { Text("Cancel") }
            }
        )
    }

    editingBookmark?.let { target ->
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text(target.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = target.url,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(target.url, false)
                            editingBookmark = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in New Tab")
                    }
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(target.url, true)
                            editingBookmark = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Private")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSpeedDial(target.id)
                        editingBookmark = null
                    }
                ) {
                    Text("Remove Pin", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBookmark = null }) { Text("Close") }
            }
        )
    }

    selectedMostVisited?.let { site ->
        val alreadyPinned = speedDials.any {
            it.url.equals(site.url, ignoreCase = true) ||
                runCatching { Uri.parse(it.url).host?.removePrefix("www.") }.getOrNull().equals(site.host, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { selectedMostVisited = null },
            title = { Text(site.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${site.host} • ${site.visitCount} ${if (site.visitCount == 1) "visit" else "visits"}",
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = {
                            onPinMostVisited(site)
                            selectedMostVisited = null
                        },
                        enabled = !alreadyPinned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("most_visited_pin_action")
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (alreadyPinned) "Already in Pinned Favorites" else "Pin to Favorites")
                    }
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(site.url, false)
                            selectedMostVisited = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in New Tab")
                    }
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(site.url, true)
                            selectedMostVisited = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Private")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissMostVisited(site.host)
                        selectedMostVisited = null
                    },
                    modifier = Modifier.testTag("most_visited_dismiss_action")
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMostVisited = null }) { Text("Cancel") }
            }
        )
    }

    selectedArticleForMenu?.let { article ->
        AlertDialog(
            onDismissRequest = { selectedArticleForMenu = null },
            title = { Text(article.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(article.url, false)
                            selectedArticleForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in New Tab")
                    }
                    OutlinedButton(
                        onClick = {
                            onOpenInNewTab(article.url, true)
                            selectedArticleForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Private")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedArticleForMenu = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ModePillItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(),
        label = "mode_pill_bg_$tag"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "mode_pill_fg_$tag"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bgColor,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MostVisitedTile(
    site: MostVisitedSite,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("most_visited_${site.host}")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FavoriteSiteIcon(
                    title = site.title,
                    url = site.url,
                    size = 36.dp
                )
                Text(
                    text = site.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (site.visitCount >= 2) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${site.visitCount}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialTile(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            )
        ),
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("speed_dial_${bookmark.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            FavoriteSiteIcon(
                title = bookmark.title,
                url = bookmark.url,
                size = 36.dp
            )
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddFavoriteGridTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.testTag("speed_dial_add_grid_tile")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(11.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Pin new favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddSpeedDialDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pinned Favorite") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (e.g. wikipedia.org)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { url }, url) },
                enabled = url.isNotBlank()
            ) {
                Text("Pin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
