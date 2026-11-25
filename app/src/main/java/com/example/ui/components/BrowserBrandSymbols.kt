package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.IntegrationInstructions
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.ShieldEmerald
import kotlin.math.abs

/**
 * Sculptural luminous hero emblem displayed on the browser Start Page.
 * Harmonizes with the adaptive launcher icon in Standard mode, and shifts
 * into a faceted amethyst compass-shield glow in Private (Incognito) mode.
 */
@Composable
fun BrowserHeroEmblem(
    isIncognito: Boolean,
    modifier: Modifier = Modifier
) {
    val glowPrimary = if (isIncognito) Color(0xFFA855F7) else Color(0xFFFF5E3A)
    val glowSecondary = if (isIncognito) Color(0xFF6366F1) else Color(0xFFFBBF24)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowPrimary.copy(alpha = 0.28f),
                            glowSecondary.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.78f
                    )
                )
            }
    ) {
        if (!isIncognito) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF080C15))
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFBBF24).copy(alpha = 0.78f),
                                Color(0xFFFF5E3A).copy(alpha = 0.72f),
                                Color(0xFFF43F5E).copy(alpha = 0.60f)
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fevro_browser_icon_1795584654540),
                    contentDescription = "Fevro Browser icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp)
                        .clip(CircleShape)
                )
            }
        } else {
            // Custom-drawn luminous Incognito celestial shield-compass emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E1338), Color(0xFF0A0614))
                        )
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFC084FC).copy(alpha = 0.8f),
                                Color(0xFF818CF8).copy(alpha = 0.5f),
                                Color(0xFF6366F1).copy(alpha = 0.7f)
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.44f

                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFC084FC),
                                Color(0xFF818CF8),
                                Color(0xFFE879F9),
                                Color(0xFFC084FC)
                            )
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4-point private compass star
                    val star = Path().apply {
                        moveTo(cx, cy - r * 0.72f)
                        lineTo(cx + r * 0.18f, cy - r * 0.18f)
                        lineTo(cx + r * 0.72f, cy)
                        lineTo(cx + r * 0.18f, cy + r * 0.18f)
                        lineTo(cx, cy + r * 0.72f)
                        lineTo(cx - r * 0.18f, cy + r * 0.18f)
                        lineTo(cx - r * 0.72f, cy)
                        lineTo(cx - r * 0.18f, cy - r * 0.18f)
                        close()
                    }
                    drawPath(
                        path = star,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF5D0FE), Color(0xFFA855F7), Color(0xFF6366F1))
                        )
                    )
                }
            }
        }
    }
}

private data class SiteVisualIdentity(
    val startColor: Color,
    val endColor: Color,
    val icon: ImageVector?
)

private fun resolveSiteVisualIdentity(host: String, url: String): SiteVisualIdentity {
    val h = host.lowercase()
    return when {
        h.contains("wikipedia.org") -> SiteVisualIdentity(Color(0xFF60A5FA), Color(0xFF2563EB), Icons.Rounded.MenuBook)
        h.contains("duckduckgo.com") -> SiteVisualIdentity(Color(0xFFFB923C), Color(0xFFEA580C), Icons.Rounded.TravelExplore)
        h.contains("github.com") -> SiteVisualIdentity(Color(0xFF94A3B8), Color(0xFF475569), Icons.Rounded.Code)
        h.contains("ycombinator.com") -> SiteVisualIdentity(Color(0xFFF97316), Color(0xFFEA580C), Icons.Rounded.Bolt)
        h.contains("mozilla.org") -> SiteVisualIdentity(Color(0xFF38BDF8), Color(0xFF0284C7), Icons.Rounded.Terminal)
        h.contains("reddit.com") -> SiteVisualIdentity(Color(0xFFFF6B4A), Color(0xFFDC2626), Icons.Rounded.Forum)
        h.contains("youtube.com") || h.contains("youtu.be") -> SiteVisualIdentity(Color(0xFFF43F5E), Color(0xFFBE123C), Icons.Rounded.PlayArrow)
        h.contains("google.") -> SiteVisualIdentity(Color(0xFF38BDF8), Color(0xFF2563EB), Icons.Rounded.Search)
        h.contains("brave.com") -> SiteVisualIdentity(Color(0xFFFB7185), Color(0xFFEA580C), Icons.Rounded.Shield)
        h.contains("bing.com") -> SiteVisualIdentity(Color(0xFF2DD4BF), Color(0xFF0284C7), Icons.Rounded.Explore)
        h.contains("stackoverflow.com") -> SiteVisualIdentity(Color(0xFFFBBF24), Color(0xFFD97706), Icons.Rounded.IntegrationInstructions)
        h.contains("amazon.") || h.contains("ebay.") || h.contains("shop") -> SiteVisualIdentity(Color(0xFFFBBF24), Color(0xFFD97706), Icons.Rounded.ShoppingBag)
        h.contains("spotify.") || h.contains("music.") -> SiteVisualIdentity(Color(0xFF34D399), Color(0xFF059669), Icons.Rounded.MusicNote)
        h.contains("linkedin.com") -> SiteVisualIdentity(Color(0xFF38BDF8), Color(0xFF0369A1), Icons.Rounded.WorkOutline)
        h.contains("instagram.com") -> SiteVisualIdentity(Color(0xFFEC4899), Color(0xFFF59E0B), Icons.Rounded.PhotoCamera)
        h.contains("x.com") || h.contains("twitter.com") -> SiteVisualIdentity(Color(0xFF7DD3FC), Color(0xFF38BDF8), Icons.Rounded.Tag)
        h.contains("news") || h.contains("bbc.") || h.contains("cnn.") || h.contains("reuters.") -> SiteVisualIdentity(Color(0xFF818CF8), Color(0xFF4F46E5), Icons.Rounded.Newspaper)
        else -> {
            val palettes = listOf(
                Color(0xFF38BDF8) to Color(0xFF2563EB),
                Color(0xFF34D399) to Color(0xFF059669),
                Color(0xFFA78BFA) to Color(0xFF7C3AED),
                Color(0xFFFBBF24) to Color(0xFFD97706),
                Color(0xFFFB7185) to Color(0xFFE11D48),
                Color(0xFF22D3EE) to Color(0xFF0891B2)
            )
            val (start, end) = palettes[abs(url.hashCode()) % palettes.size]
            SiteVisualIdentity(start, end, null)
        }
    }
}

/**
 * Renders a crisp high-resolution site favicon via Coil inside a luminous squircle,
 * falling back seamlessly to a curated jewel-toned symbol or sculpted monogram badge.
 */
@Composable
fun FavoriteSiteIcon(
    title: String,
    url: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val host = remember(url) {
        runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull().orEmpty()
    }
    val identity = remember(host, url) { resolveSiteVisualIdentity(host, url) }
    val initial = remember(title, host) {
        (title.trim().firstOrNull() ?: host.firstOrNull() ?: 'W').uppercaseChar().toString()
    }
    val faviconUrl = remember(host) {
        if (host.isNotBlank()) "https://www.google.com/s2/favicons?domain=$host&sz=128" else null
    }
    val outerCorner = (size * 0.30f).coerceIn(10.dp, 17.dp)
    val plateSize = (size * 0.76f).coerceAtLeast(22.dp)
    val plateCorner = (size * 0.21f).coerceIn(7.dp, 11.dp)
    val platePadding = (size * 0.10f).coerceIn(3.5.dp, 5.dp)
    val shape = RoundedCornerShape(outerCorner)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        identity.startColor.copy(alpha = 0.24f),
                        identity.endColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        identity.startColor.copy(alpha = 0.48f),
                        identity.endColor.copy(alpha = 0.16f)
                    )
                ),
                shape = shape
            )
    ) {
        if (faviconUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(faviconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$title icon",
                contentScale = ContentScale.Fit,
                loading = {
                    FallbackSiteGlyph(identity = identity, initial = initial, iconSize = size)
                },
                error = {
                    FallbackSiteGlyph(identity = identity, initial = initial, iconSize = size)
                },
                success = { state ->
                    // Soft translucent plate ensures dark brand favicons stay luminous on AMOLED black
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(plateSize)
                            .clip(RoundedCornerShape(plateCorner))
                            .background(Color.White.copy(alpha = 0.92f))
                            .padding(platePadding)
                    ) {
                        Image(
                            painter = state.painter,
                            contentDescription = "$title icon",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FallbackSiteGlyph(identity = identity, initial = initial, iconSize = size)
        }
    }
}

@Composable
private fun FallbackSiteGlyph(
    identity: SiteVisualIdentity,
    initial: String,
    iconSize: Dp
) {
    val glyphSize = (iconSize * 0.48f).coerceIn(17.dp, 26.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (identity.icon != null) {
            Icon(
                imageVector = identity.icon,
                contentDescription = null,
                tint = identity.startColor,
                modifier = Modifier.size(glyphSize)
            )
        } else {
            Text(
                text = initial,
                style = if (iconSize <= 42.dp) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                },
                color = identity.startColor
            )
        }
    }
}
