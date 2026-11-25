package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ReaderFontChoice
import com.example.data.ReaderLineSpacing
import com.example.data.ReaderPaperTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.ReaderArticle
import kotlin.math.roundToInt

@Composable
fun ReaderViewOverlay(
    article: ReaderArticle?,
    isLoading: Boolean,
    fontSizeSp: Int,
    paperTheme: ReaderPaperTheme,
    fontChoice: ReaderFontChoice,
    lineSpacing: ReaderLineSpacing,
    onFontSizeChange: (Int) -> Unit,
    onPaperThemeChange: (ReaderPaperTheme) -> Unit,
    onFontChoiceChange: (ReaderFontChoice) -> Unit,
    onLineSpacingChange: (ReaderLineSpacing) -> Unit,
    onShareArticle: (title: String, url: String) -> Unit,
    onCopiedArticle: () -> Unit,
    onClose: () -> Unit
) {
    var showTypographyBar by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val (paperBg, paperInk, paperMuted, quoteBg) = when (paperTheme) {
        ReaderPaperTheme.LIGHT -> QuadColor(Color(0xFFFAFAFA), Color(0xFF0F172A), Color(0xFF475569), Color(0xFFEEF2F6))
        ReaderPaperTheme.SEPIA -> QuadColor(Color(0xFFF6EFE0), Color(0xFF342718), Color(0xFF6B563E), Color(0xFFEBDFC7))
        ReaderPaperTheme.DARK -> QuadColor(Color(0xFF111827), Color(0xFFE5E7EB), Color(0xFF9CA3AF), Color(0xFF1F2937))
        ReaderPaperTheme.AMOLED -> QuadColor(Color(0xFF000000), Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF11141C))
    }

    val bodyFontFamily = when (fontChoice) {
        ReaderFontChoice.SANS -> PlusJakartaSansFontFamily
        ReaderFontChoice.SERIF -> FontFamily.Serif
        ReaderFontChoice.MONO -> JetBrainsMonoFontFamily
    }
    val headingFontFamily = when (fontChoice) {
        ReaderFontChoice.SERIF -> FontFamily.Serif
        ReaderFontChoice.MONO -> JetBrainsMonoFontFamily
        ReaderFontChoice.SANS -> SpaceGroteskFontFamily
    }

    val readProgress by remember(article) {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (article == null || totalItems <= 1) 0f
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        containerColor = paperBg,
        topBar = {
            Surface(
                color = paperBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("reader_close_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Reading Mode",
                                tint = paperInk
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reading Mode",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = paperInk,
                                maxLines = 1
                            )
                            if (article != null) {
                                val pct = (readProgress * 100f).roundToInt()
                                Text(
                                    text = "${article.readingMinutes} min read • $pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = paperMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (article != null) {
                            IconButton(
                                onClick = {
                                    val plainText = buildString {
                                        appendLine(article.title)
                                        if (article.byline.isNotBlank()) appendLine("By ${article.byline}")
                                        appendLine()
                                        article.blocks.forEach { b ->
                                            if (b.kind == "list_item") append("• ")
                                            appendLine(b.text)
                                            appendLine()
                                        }
                                    }
                                    clipboard.setText(AnnotatedString(plainText.trim()))
                                    onCopiedArticle()
                                },
                                modifier = Modifier.testTag("reader_copy_button")
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy clean article text",
                                    tint = paperInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onShareArticle(article.title, article.url) },
                                modifier = Modifier.testTag("reader_share_button")
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Share article",
                                    tint = paperInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showTypographyBar = !showTypographyBar },
                            modifier = Modifier.testTag("reader_typography_button")
                        ) {
                            Icon(
                                Icons.Filled.FormatSize,
                                contentDescription = "Customize Reading Mode typography",
                                tint = if (showTypographyBar) MaterialTheme.colorScheme.primary else paperInk
                            )
                        }
                    }

                    // Live Reading Progress Hairline
                    if (article != null) {
                        LinearProgressIndicator(
                            progress = { readProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = paperMuted.copy(alpha = 0.14f)
                        )
                    } else {
                        HorizontalDivider(color = paperMuted.copy(alpha = 0.2f))
                    }

                    // Expandable Editorial Typography & Paper Theme Panel
                    AnimatedVisibility(
                        visible = showTypographyBar,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(quoteBg)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Paper Theme Color Swatches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Paper Theme", style = MaterialTheme.typography.labelMedium, color = paperInk)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ReaderPaperTheme.entries.forEach { preset ->
                                        val swatchBg = when (preset) {
                                            ReaderPaperTheme.LIGHT -> Color(0xFFFAFAFA)
                                            ReaderPaperTheme.SEPIA -> Color(0xFFF6EFE0)
                                            ReaderPaperTheme.DARK -> Color(0xFF1E293B)
                                            ReaderPaperTheme.AMOLED -> Color(0xFF000000)
                                        }
                                        val swatchCheck = when (preset) {
                                            ReaderPaperTheme.LIGHT, ReaderPaperTheme.SEPIA -> Color(0xFF0F172A)
                                            ReaderPaperTheme.DARK, ReaderPaperTheme.AMOLED -> Color(0xFFF8FAFC)
                                        }
                                        val selected = paperTheme == preset
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(swatchBg)
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else paperMuted.copy(alpha = 0.45f),
                                                    shape = CircleShape
                                                )
                                                .clickable { onPaperThemeChange(preset) }
                                                .testTag("reader_theme_${preset.name.lowercase()}")
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = preset.label,
                                                    tint = swatchCheck,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Font Family & Line Spacing Selectors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ReaderFontChoice.entries.forEach { choice ->
                                    FilterChip(
                                        selected = fontChoice == choice,
                                        onClick = { onFontChoiceChange(choice) },
                                        label = { Text(choice.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reader_font_${choice.name.lowercase()}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ReaderLineSpacing.entries.forEach { spacing ->
                                    FilterChip(
                                        selected = lineSpacing == spacing,
                                        onClick = { onLineSpacingChange(spacing) },
                                        label = { Text(spacing.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reader_spacing_${spacing.name.lowercase()}")
                                    )
                                }
                            }

                            // 3. Font Size Stepper + Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onFontSizeChange((fontSizeSp - 1).coerceAtLeast(14)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("A-", color = paperInk, style = MaterialTheme.typography.labelMedium)
                                }
                                Slider(
                                    value = fontSizeSp.toFloat(),
                                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                                    valueRange = 14f..24f,
                                    steps = 9,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onFontSizeChange((fontSizeSp + 1).coerceAtMost(24)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("A+", color = paperInk, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    text = "${fontSizeSp}sp",
                                    style = MonoCodeStyle,
                                    color = paperInk
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Stripping ads & sidebars...", color = paperMuted)
                    }
                }
                article == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Article,
                            contentDescription = null,
                            tint = paperMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Readable Article Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = paperInk
                        )
                        Text(
                            text = "This page doesn't have long-form article text to distill. Try Reading Mode on news stories, blogs, Wikipedia, or documentation pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = paperMuted
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .testTag("reader_article_list"),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item(key = "reader_header") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Site Kicker + Stripped Clutter Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${article.siteName.uppercase()} • ${article.readingMinutes} MIN READ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = paperMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (article.strippedClutterCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ShieldEmerald.copy(alpha = 0.14f),
                                            modifier = Modifier.testTag("reader_clutter_badge")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Shield,
                                                    contentDescription = null,
                                                    tint = ShieldEmerald,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${article.strippedClutterCount} stripped",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = ShieldEmerald
                                                )
                                            }
                                        }
                                    }
                                }

                                // Editorial Headline
                                Text(
                                    text = article.title,
                                    fontFamily = headingFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (fontSizeSp + 10).sp,
                                    lineHeight = (fontSizeSp + 17).sp,
                                    color = paperInk
                                )

                                // Author & Publish Date Metadata
                                val metaLine = listOfNotNull(
                                    article.byline.takeIf { it.isNotBlank() }?.let { "By $it" },
                                    article.publishedDate.takeIf { it.isNotBlank() }
                                ).joinToString("  •  ")
                                if (metaLine.isNotBlank()) {
                                    Text(
                                        text = metaLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = paperMuted
                                    )
                                }

                                // Lead Hero Image (if available and valid HTTPS)
                                if (article.leadImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = article.leadImageUrl,
                                        contentDescription = article.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(quoteBg)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 6.dp),
                                    color = paperMuted.copy(alpha = 0.22f)
                                )
                            }
                        }

                        items(article.blocks) { block ->
                            val effectiveLineHeight = (fontSizeSp * lineSpacing.multiplier).sp
                            when (block.kind) {
                                "heading" -> {
                                    Text(
                                        text = block.text,
                                        fontFamily = headingFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (fontSizeSp + 4).sp,
                                        lineHeight = (fontSizeSp + 11).sp,
                                        color = paperInk,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                "quote" -> {
                                    Surface(
                                        color = quoteBg,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(48.dp)
                                                    .align(Alignment.CenterVertically)
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                            Text(
                                                text = block.text,
                                                fontFamily = bodyFontFamily,
                                                fontStyle = FontStyle.Italic,
                                                fontSize = fontSizeSp.sp,
                                                lineHeight = effectiveLineHeight,
                                                color = paperInk,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                            )
                                        }
                                    }
                                }
                                "list_item" -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = (fontSizeSp * 0.55f).dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = block.text,
                                            fontFamily = bodyFontFamily,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = effectiveLineHeight,
                                            color = paperInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                "code" -> {
                                    Surface(
                                        color = quoteBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, paperMuted.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = block.text,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = (fontSizeSp - 3).coerceAtLeast(12).sp,
                                            lineHeight = (fontSizeSp + 2).sp,
                                            color = paperInk,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = block.text,
                                        fontFamily = bodyFontFamily,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = effectiveLineHeight,
                                        color = paperInk
                                    )
                                }
                            }
                        }

                        item(key = "reader_footer") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = paperMuted.copy(alpha = 0.22f))
                                Text(
                                    text = "End of article • ${article.wordCount} words",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = paperMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewSourceOverlay(
    url: String,
    sourceCode: String,
    isLoading: Boolean,
    onClose: () -> Unit,
    onCopied: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var filterText by remember { mutableStateOf("") }

    val displayedText = remember(sourceCode, filterText) {
        if (filterText.isBlank()) {
            sourceCode.take(60_000)
        } else {
            sourceCode.lineSequence()
                .filter { it.contains(filterText, ignoreCase = true) }
                .take(300)
                .joinToString("\n")
                .ifBlank { "No matching lines for \"$filterText\"" }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("view_source_close")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close page source")
                        }
                        Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Page Source", style = MaterialTheme.typography.titleMedium)
                            Text(url, style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(sourceCode))
                                onCopied()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy HTML source")
                        }
                    }
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Filter HTML lines...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Text(
                    text = displayedText.ifBlank { "No HTML source available." },
                    style = MonoCodeStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }
        }
    }
}

private data class QuadColor(
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val surface: Color
)
