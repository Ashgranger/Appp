package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EncryptedPasswordStore
import com.example.data.SavedCredential
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PasswordVaultScreen(
    credentials: List<SavedCredential>,
    neverSaveHosts: Set<String>,
    onSaveCredential: (host: String, username: String, password: String, existingId: String?) -> Unit,
    onDeleteCredential: (String) -> Unit,
    onRemoveNeverSaveHost: (String) -> Unit,
    onClearAll: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopiedMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var editingCredential by remember { mutableStateOf<SavedCredential?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filtered = remember(credentials, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) credentials
        else credentials.filter {
            it.host.contains(q, ignoreCase = true) ||
                it.username.contains(q, ignoreCase = true) ||
                it.originUrl.contains(q, ignoreCase = true)
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("password_vault_close")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Password Vault")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Password Vault",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${credentials.size} saved • AES-256 local encryption",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldEmerald
                            )
                        }
                        if (credentials.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.testTag("password_vault_clear_all")
                            ) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear all passwords",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Button(
                            onClick = { showAddDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("password_vault_add_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search sites or usernames...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 12.dp)
                            .testTag("password_vault_search")
                    )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Local-only AES-256 info strip
            item(key = "vault_security_badge") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, ShieldEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = ShieldEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Stored exclusively on this device with Android EncryptedSharedPreferences (AES-256-GCM). Never synced to the cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "vault_empty_state") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                ShieldEmerald.copy(alpha = 0.24f),
                                                ShieldEmerald.copy(alpha = 0.05f)
                                            )
                                        )
                                    )
                                    .border(1.dp, ShieldEmerald.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = ShieldEmerald,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isBlank()) "Your Local Vault is Ready" else "No Matching Logins",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "Sign in on any website to save credentials automatically, or add a login manually with the strong password generator."
                                } else {
                                    "Try another site name or username."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isBlank()) {
                                Button(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Login Manually")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { cred ->
                    CredentialCard(
                        credential = cred,
                        onCopyUsername = {
                            clipboard.setText(AnnotatedString(cred.username))
                            onCopiedMessage("Copied username")
                        },
                        onCopyPassword = {
                            clipboard.setText(AnnotatedString(cred.password))
                            onCopiedMessage("Copied password")
                        },
                        onEdit = { editingCredential = cred },
                        onDelete = { onDeleteCredential(cred.id) },
                        onOpenSite = {
                            onOpenUrl(cred.originUrl.ifBlank { "https://${cred.host}" })
                        }
                    )
                }
            }

            if (neverSaveHosts.isNotEmpty()) {
                item(key = "never_save_header") {
                    Text(
                        text = "Never Save Sites (${neverSaveHosts.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item(key = "never_save_card") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            neverSaveHosts.sorted().forEachIndexed { idx, host ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = host,
                                        style = MonoCodeStyle,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onRemoveNeverSaveHost(host) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Allow saving for $host",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (idx < neverSaveHosts.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showAddDialog) {
        AddOrEditCredentialDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { host, user, pass ->
                onSaveCredential(host, user, pass, null)
                showAddDialog = false
            }
        )
    }

    editingCredential?.let { target ->
        AddOrEditCredentialDialog(
            existing = target,
            onDismiss = { editingCredential = null },
            onSave = { host, user, pass ->
                onSaveCredential(host, user, pass, target.id)
                editingCredential = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete All Saved Passwords?") },
            text = { Text("This permanently removes all ${credentials.size} encrypted credentials from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CredentialCard(
    credential: SavedCredential,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSite: () -> Unit
) {
    var passwordVisible by remember(credential.id) { mutableStateOf(false) }

    val palette = listOf(
        Color(0xFF2563EB) to Color(0xFF60A5FA),
        Color(0xFF059669) to Color(0xFF34D399),
        Color(0xFF7C3AED) to Color(0xFFA78BFA),
        Color(0xFFD97706) to Color(0xFFFBBF24),
        Color(0xFFE11D48) to Color(0xFFFB7185),
        Color(0xFF0891B2) to Color(0xFF22D3EE)
    )
    val (c1, c2) = palette[abs(credential.host.hashCode()) % palette.size]
    val initial = credential.host.firstOrNull()?.uppercaseChar()?.toString() ?: "K"

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credential_card_${credential.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(c1.copy(alpha = 0.22f), c2.copy(alpha = 0.14f))))
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c1
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credential.host,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = credential.username.ifBlank { "(No username)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onOpenSite, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = "Open website", modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit credential", modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete credential",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Password Preview & Quick Copy Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (passwordVisible) credential.password else "•".repeat(credential.password.length.coerceIn(8, 16)),
                        style = MonoCodeStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (credential.username.isNotBlank()) {
                        IconButton(onClick = onCopyUsername, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Person, contentDescription = "Copy username", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onCopyPassword, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy password",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditCredentialDialog(
    existing: SavedCredential?,
    onDismiss: () -> Unit,
    onSave: (host: String, username: String, password: String) -> Unit
) {
    var host by remember { mutableStateOf(existing?.host.orEmpty()) }
    var username by remember { mutableStateOf(existing?.username.orEmpty()) }
    var password by remember { mutableStateOf(existing?.password.orEmpty()) }
    var showPassword by remember { mutableStateOf(existing == null) }
    var genLength by remember { mutableFloatStateOf(18f) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Encrypted Login" else "Edit Login") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Website (e.g. github.com)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_dialog_host")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_dialog_username")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_dialog_password")
                )

                // Built-in Strong Password Generator
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generator (${genLength.roundToInt()} chars)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TextButton(
                                onClick = {
                                    password = EncryptedPasswordStore.generateStrongPassword(
                                        length = genLength.roundToInt(),
                                        includeUppercase = useUpper,
                                        includeDigits = useDigits,
                                        includeSymbols = useSymbols
                                    )
                                    showPassword = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("vault_generate_password_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Generate")
                            }
                        }
                        Slider(
                            value = genLength,
                            onValueChange = { genLength = it },
                            valueRange = 12f..32f,
                            steps = 19
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = useUpper,
                                onClick = { useUpper = !useUpper },
                                label = { Text("A-Z", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = useDigits,
                                onClick = { useDigits = !useDigits },
                                label = { Text("0-9", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = useSymbols,
                                onClick = { useSymbols = !useSymbols },
                                label = { Text("!@#", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(host, username, password) },
                enabled = host.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.testTag("vault_dialog_save")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
