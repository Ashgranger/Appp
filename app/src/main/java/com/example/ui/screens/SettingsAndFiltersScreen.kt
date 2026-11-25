package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BrowserPreferencesState
import com.example.data.FilterRuleEntity
import com.example.data.FilterSubscriptionEntity
import com.example.data.FilterUpdateCadence
import com.example.data.ImageLoadingPolicy
import com.example.data.OmniboxPosition
import com.example.data.ShieldAllowlistEntity
import com.example.data.TabLayoutMode
import com.example.data.ThemeMode
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.viewmodel.SearchEngine
import com.example.viewmodel.SettingsSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsAndFiltersScreen(
    section: SettingsSection,
    preferences: BrowserPreferencesState,
    searchEngines: List<SearchEngine>,
    filterSubscriptions: List<FilterSubscriptionEntity>,
    userCustomRules: List<FilterRuleEntity>,
    allowlistedHosts: List<ShieldAllowlistEntity>,
    isUpdatingFilters: Boolean,
    activeRuleCount: Int,
    savedPasswordCount: Int,
    onSectionChange: (SettingsSection) -> Unit,
    onUpdatePreferences: ((BrowserPreferencesState) -> BrowserPreferencesState) -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onAddCustomSearchEngine: (name: String, templateUrl: String) -> Unit,
    onDeleteCustomSearchEngine: (String) -> Unit,
    onToggleFilterSubscription: (String, Boolean) -> Unit,
    onAddCustomFilterSubscription: (name: String, url: String) -> Unit,
    onDeleteCustomFilterSubscription: (String) -> Unit,
    onUpdateFilterListsNow: () -> Unit,
    onAddCustomBlockedDomain: (String) -> Unit,
    onRemoveCustomBlockedDomain: (String) -> Unit,
    onRemoveAllowlistedHost: (String) -> Unit,
    onRestoreDismissedMostVisited: () -> Unit,
    onOpenPasswordVault: () -> Unit,
    onOpenFeedback: () -> Unit,
    onClearAllBrowsingData: () -> Unit,
    onClose: () -> Unit
) {
    var showAddEngineDialog by remember { mutableStateOf(false) }
    var showAddFilterSubDialog by remember { mutableStateOf(false) }
    var showAddBlockRuleDialog by remember { mutableStateOf(false) }
    var homepageDraft by remember(preferences.customHomepageUrl) { mutableStateOf(preferences.customHomepageUrl) }

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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("settings_close_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close settings")
                        }
                        Image(
                            painter = painterResource(id = R.drawable.fevro_browser_icon),
                            contentDescription = "Fevro Browser icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Column {
                            Text("Fevro Browser", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "Customization & Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = section.ordinal,
                        edgePadding = 12.dp
                    ) {
                        SettingsSection.entries.forEach { sec ->
                            Tab(
                                selected = section == sec,
                                onClick = { onSectionChange(sec) },
                                text = { Text(sec.label) },
                                modifier = Modifier.testTag("settings_tab_${sec.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (section) {
                SettingsSection.APPEARANCE -> {
                    item { SectionTitle("Theme Mode") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = { onUpdatePreferences { it.copy(themeMode = mode) } },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            SettingSwitchRow(
                                title = "Material You Dynamic Color",
                                subtitle = "Adapt browser chrome to your Android wallpaper",
                                checked = preferences.useDynamicColor,
                                onCheckedChange = { v -> onUpdatePreferences { it.copy(useDynamicColor = v) } }
                            )
                        }
                    }

                    item { SectionTitle("Accent Palette") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AccentPreset.entries.forEach { preset ->
                                val selected = preferences.accentPreset == preset && !preferences.useDynamicColor
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(preset.lightPrimary)
                                        .then(
                                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            onUpdatePreferences { it.copy(accentPreset = preset, useDynamicColor = false) }
                                        }
                                        .testTag("accent_${preset.name.lowercase()}")
                                ) {
                                    if (selected) {
                                        Icon(Icons.Filled.Check, contentDescription = preset.displayName, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }

                    item { SectionTitle("Start Page & Speed Dial") }
                    item {
                        SettingSwitchRow(
                            title = "Show Most Visited Sites",
                            subtitle = "Display your top visited sites above Pinned Favorites",
                            checked = preferences.startPageShowMostVisited,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(startPageShowMostVisited = v) } },
                            tag = "setting_show_most_visited"
                        )
                    }
                    if (preferences.dismissedMostVisitedHosts.isNotEmpty()) {
                        item {
                            OutlinedButton(
                                onClick = onRestoreDismissedMostVisited,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_restore_most_visited")
                            ) {
                                Text("Restore ${preferences.dismissedMostVisitedHosts.size} Dismissed Most Visited Site(s)")
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Speed Dial Columns: ${preferences.speedDialColumns}", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(3, 4, 5).forEach { cols ->
                                        FilterChip(
                                            selected = preferences.speedDialColumns == cols,
                                            onClick = { onUpdatePreferences { it.copy(speedDialColumns = cols) } },
                                            label = { Text("$cols Columns") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Custom Homepage URL (Optional)", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = homepageDraft,
                                        onValueChange = { homepageDraft = it },
                                        placeholder = { Text("Leave blank for Speed Dial") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(onClick = { onUpdatePreferences { it.copy(customHomepageUrl = homepageDraft.trim()) } }) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenFeedback,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_feedback_button")
                        ) {
                            Icon(Icons.Filled.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Quick Feedback")
                        }
                    }
                }

                SettingsSection.LAYOUT -> {
                    item { SectionTitle("Address Bar Position") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OmniboxPosition.entries.forEach { pos ->
                                SelectableOptionCard(
                                    title = pos.label,
                                    subtitle = if (pos == OmniboxPosition.BOTTOM) "Thumb-friendly bottom reach toolbar" else "Traditional top-anchored omnibox layout",
                                    selected = preferences.omniboxPosition == pos,
                                    onClick = { onUpdatePreferences { it.copy(omniboxPosition = pos) } },
                                    tag = "omnibox_pos_${pos.name.lowercase()}"
                                )
                            }
                        }
                    }

                    item { SectionTitle("Toolbar Behavior") }
                    item {
                        SettingSwitchRow(
                            title = "Auto-Hide Toolbar on Scroll",
                            subtitle = "Maximize reading viewport when scrolling down pages",
                            checked = preferences.autoHideToolbarOnScroll,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(autoHideToolbarOnScroll = v) } }
                        )
                    }

                    item { SectionTitle("Default Tab Switcher View") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            TabLayoutMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.tabLayoutMode == mode,
                                    onClick = { onUpdatePreferences { it.copy(tabLayoutMode = mode) } },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                SettingsSection.SPEED -> {
                    item { SectionTitle("Rendering Engine Optimization") }
                    item {
                        SettingSwitchRow(
                            title = "Turbo Speed Mode",
                            subtitle = "Strips heavy ad iframes, prioritizes core scripts, and tunes Chromium cache",
                            checked = preferences.turboModeEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(turboModeEnabled = v) } },
                            tag = "setting_turbo_mode"
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "CSS & Layout Acceleration",
                            subtitle = "Injects content-visibility: auto hints to speed up long-page painting",
                            checked = preferences.cssPerfOptimizationEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(cssPerfOptimizationEnabled = v) } }
                        )
                    }

                    item { SectionTitle("Image Loading Policy") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImageLoadingPolicy.entries.forEach { policy ->
                                SelectableOptionCard(
                                    title = policy.label,
                                    subtitle = policy.subtitle,
                                    selected = preferences.imageLoadingPolicy == policy,
                                    onClick = { onUpdatePreferences { it.copy(imageLoadingPolicy = policy) } }
                                )
                            }
                        }
                    }

                    item { SectionTitle("JavaScript, Zoom & Display") }
                    item {
                        SettingSwitchRow(
                            title = "Enable JavaScript",
                            subtitle = "Disable for extreme speed and strict static reading",
                            checked = preferences.javaScriptEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(javaScriptEnabled = v) } }
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Force Dark Web Pages",
                            subtitle = "Apply smart dark rendering on bright websites at night",
                            checked = preferences.forceDarkWebPages,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(forceDarkWebPages = v) } }
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Request Desktop Sites by Default",
                            subtitle = "Open new tabs with full desktop viewport and User-Agent",
                            checked = preferences.defaultDesktopMode,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(defaultDesktopMode = v) } }
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Default Page Text Zoom", style = MaterialTheme.typography.titleSmall)
                                    Text("${preferences.defaultTextZoomPercent}%", style = MonoCodeStyle)
                                }
                                Slider(
                                    value = preferences.defaultTextZoomPercent.toFloat(),
                                    onValueChange = { v ->
                                        onUpdatePreferences { it.copy(defaultTextZoomPercent = (v / 10f).roundToInt() * 10) }
                                    },
                                    valueRange = 50f..200f,
                                    steps = 14
                                )
                            }
                        }
                    }
                }

                SettingsSection.SHIELD -> {
                    item { SectionTitle("SwiftShield Ad & Tracker Blocker") }
                    item {
                        SettingSwitchRow(
                            title = "Enable SwiftShield Globally",
                            subtitle = "$activeRuleCount active host blocking rules in memory",
                            checked = preferences.shieldEnabledGlobally,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(shieldEnabledGlobally = v) } },
                            tag = "setting_shield_global"
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Cosmetic Ad Hiding & Slot Collapse",
                            subtitle = "Hide leftover ad containers, sponsored cards, and collapse empty gaps dynamically",
                            checked = preferences.cosmeticBlockingEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(cosmeticBlockingEnabled = v) } },
                            tag = "setting_cosmetic_blocking"
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Strip URL Tracking Parameters",
                            subtitle = "Automatically clean utm_*, fbclid, gclid, and referral tags from links",
                            checked = preferences.stripTrackingParamsEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(stripTrackingParamsEnabled = v) } },
                            tag = "setting_strip_tracking_params"
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Block Popups & Ad Redirects",
                            subtitle = "Stop background popunder windows and malicious click-hijack redirects",
                            checked = preferences.blockPopupsAndRedirects,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(blockPopupsAndRedirects = v) } },
                            tag = "setting_block_popups"
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Blocking Categories", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = preferences.blockAds,
                                        onClick = { onUpdatePreferences { it.copy(blockAds = !it.blockAds) } },
                                        label = { Text("Ads") }
                                    )
                                    FilterChip(
                                        selected = preferences.blockTrackers,
                                        onClick = { onUpdatePreferences { it.copy(blockTrackers = !it.blockTrackers) } },
                                        label = { Text("Trackers") }
                                    )
                                    FilterChip(
                                        selected = preferences.blockSocial,
                                        onClick = { onUpdatePreferences { it.copy(blockSocial = !it.blockSocial) } },
                                        label = { Text("Social") }
                                    )
                                    FilterChip(
                                        selected = preferences.blockAnnoyances,
                                        onClick = { onUpdatePreferences { it.copy(blockAnnoyances = !it.blockAnnoyances) } },
                                        label = { Text("Banners") }
                                    )
                                }
                            }
                        }
                    }

                    // Auto-Updating Filter Subscriptions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle("Filter List Subscriptions")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showAddFilterSubDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add URL", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onUpdateFilterListsNow,
                                    enabled = !isUpdatingFilters,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("update_filters_now_button")
                                ) {
                                    if (isUpdatingFilters) {
                                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("Update Now", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filterSubscriptions.forEachIndexed { idx, sub ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sub.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "${sub.ruleCount} rules • Updated ${if (sub.lastUpdatedAt > 0) dateFmt.format(Date(sub.lastUpdatedAt)) else "Pending"}",
                                                style = MonoCodeStyle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!sub.isBuiltIn) {
                                            IconButton(onClick = { onDeleteCustomFilterSubscription(sub.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete custom filter list")
                                            }
                                        }
                                        Switch(
                                            checked = sub.isEnabled,
                                            onCheckedChange = { onToggleFilterSubscription(sub.id, it) }
                                        )
                                    }
                                    if (idx < filterSubscriptions.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Automatic List Update Cadence", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterUpdateCadence.entries.forEach { cadence ->
                                        FilterChip(
                                            selected = preferences.filterUpdateCadence == cadence,
                                            onClick = { onUpdatePreferences { it.copy(filterUpdateCadence = cadence) } },
                                            label = { Text(cadence.label) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Auto-update on Wi-Fi only", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = preferences.filterUpdateWifiOnly,
                                        onCheckedChange = { v -> onUpdatePreferences { it.copy(filterUpdateWifiOnly = v) } }
                                    )
                                }
                            }
                        }
                    }

                    // Custom User Rules & Site Allowlist
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle("Custom Block Rules (${userCustomRules.size})")
                            TextButton(onClick = { showAddBlockRuleDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Add Domain")
                            }
                        }
                    }
                    if (userCustomRules.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    userCustomRules.forEach { rule ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(rule.domain, style = MonoCodeStyle, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onRemoveCustomBlockedDomain(rule.domain) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Remove rule")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (allowlistedHosts.isNotEmpty()) {
                        item { SectionTitle("Paused Sites (Allowlist)") }
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    allowlistedHosts.forEach { entry ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(entry.host, style = MonoCodeStyle, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onRemoveAllowlistedHost(entry.host) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Re-enable shield for site")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search Engines
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle("Search Engine")
                            TextButton(
                                onClick = { showAddEngineDialog = true },
                                modifier = Modifier.testTag("add_custom_search_engine_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Add Custom")
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                searchEngines.forEach { engine ->
                                    val isSelected = preferences.defaultSearchEngineId == engine.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectSearchEngine(engine.id) }
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                engine.name,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(engine.searchUrlTemplate, style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = ShieldEmerald)
                                        }
                                        if (engine.isCustom) {
                                            IconButton(onClick = { onDeleteCustomSearchEngine(engine.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete custom search engine")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SettingSwitchRow(
                            title = "Live Search Suggestions",
                            subtitle = "Show real-time autocomplete from your selected search engine (paused in Incognito)",
                            checked = preferences.searchSuggestionsEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(searchSuggestionsEnabled = v) } },
                            tag = "setting_live_search_suggestions"
                        )
                    }

                    // Local Encrypted Password Manager
                    item { SectionTitle("Local Encrypted Password Manager") }
                    item {
                        SettingSwitchRow(
                            title = "Enable Password Manager & Auto-Fill",
                            subtitle = "Encrypted on this device with AES-256-GCM (never leaves device)",
                            checked = preferences.passwordManagerEnabled,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(passwordManagerEnabled = v) } },
                            tag = "setting_password_manager_enabled"
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Offer to Save Passwords",
                            subtitle = "Prompt to save login credentials when signing into sites",
                            checked = preferences.autoPromptSavePasswords,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(autoPromptSavePasswords = v) } },
                            tag = "setting_auto_prompt_passwords"
                        )
                    }
                    item {
                        Button(
                            onClick = onOpenPasswordVault,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_password_vault")
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Password Vault ($savedPasswordCount saved)")
                        }
                    }

                    // Privacy Hardening
                    item { SectionTitle("Privacy & Security") }
                    item {
                        SettingSwitchRow(
                            title = "HTTPS-Only Mode",
                            subtitle = "Automatically upgrade insecure HTTP links to encrypted HTTPS",
                            checked = preferences.httpsOnlyMode,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(httpsOnlyMode = v) } }
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Block Third-Party Cookies",
                            subtitle = "Prevent cross-site tracking cookies across domains",
                            checked = preferences.blockThirdPartyCookies,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(blockThirdPartyCookies = v) } }
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Clear History on Exit",
                            subtitle = "Automatically wipe local browsing history when leaving the browser",
                            checked = preferences.clearHistoryOnExit,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(clearHistoryOnExit = v) } }
                        )
                    }
                    item {
                        SettingSwitchRow(
                            title = "Clear Cache on Exit",
                            subtitle = "Automatically purge web cache files when closing the app",
                            checked = preferences.clearCacheOnExit,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(clearCacheOnExit = v) } }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = onClearAllBrowsingData,
                            modifier = Modifier.fillMaxWidth().testTag("settings_clear_all_data")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Clear All History, Cache & Cookies", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddEngineDialog) {
        var name by remember { mutableStateOf("") }
        var template by remember { mutableStateOf("https://kagi.com/search?q={query}") }
        AlertDialog(
            onDismissRequest = { showAddEngineDialog = false },
            title = { Text("Add Custom Search Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Engine Name") }, singleLine = true)
                    OutlinedTextField(
                        value = template,
                        onValueChange = { template = it },
                        label = { Text("Search URL (use {query})") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomSearchEngine(name, template)
                        showAddEngineDialog = false
                    },
                    enabled = name.isNotBlank() && template.isNotBlank()
                ) { Text("Add Engine") }
            },
            dismissButton = { TextButton(onClick = { showAddEngineDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddFilterSubDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFilterSubDialog = false },
            title = { Text("Add Filter List URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("List Name") }, singleLine = true)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Hosts / EasyList URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomFilterSubscription(name, url)
                        showAddFilterSubDialog = false
                    },
                    enabled = url.isNotBlank()
                ) { Text("Subscribe") }
            },
            dismissButton = { TextButton(onClick = { showAddFilterSubDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddBlockRuleDialog) {
        var domain by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBlockRuleDialog = false },
            title = { Text("Add Custom Blocked Domain") },
            text = {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain (e.g. tracker.example.com)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomBlockedDomain(domain)
                        showAddBlockRuleDialog = false
                    },
                    enabled = domain.contains('.')
                ) { Text("Block Domain") }
            },
            dismissButton = { TextButton(onClick = { showAddBlockRuleDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (tag != null) Modifier.testTag(tag) else Modifier
            )
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


