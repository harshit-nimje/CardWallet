package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PastelSand
import com.example.ui.theme.PastelSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.WalletViewModel

import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelPeach

@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val filteredCards by viewModel.filteredCards.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }

    val isGuest by viewModel.isGuestMode.collectAsState()
    val showDistanceEta by viewModel.showDistanceEta.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val cardTilt3dEnabled by viewModel.cardTilt3dEnabled.collectAsState()
    val autoGeminiOcr by viewModel.autoGeminiOcr.collectAsState()
    val useLocalAiModel by viewModel.useLocalAiModel.collectAsState()
    val compactStackView by viewModel.compactStackView.collectAsState()
    val useMaterialDynamicTheme by viewModel.useMaterialDynamicTheme.collectAsState()
    val selectedPaletteTheme by viewModel.selectedPaletteTheme.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btn_settings_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Wallet",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customization & Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // User Profile & Sync Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isGuest) PastelLavender.copy(alpha = 0.15f) else PastelSand.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isGuest) PastelLavender else PastelSand),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isGuest) "G" else (currentUser?.displayName?.firstOrNull()?.toString()
                                        ?: currentUser?.email?.firstOrNull()?.toString()?.uppercase()
                                        ?: "W"),
                                    color = if (isGuest) PastelLavender else PastelSand,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGuest) "Guest Session" else (currentUser?.displayName ?: "Wallet Account"),
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGuest) "Local on-device storage • Offline ready" else (currentUser?.email ?: "Synced with Google Account"),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGuest) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isGuest) PastelLavender else PastelSand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isGuest) "Local Wallet Mode (Exportable JSON)" else "Cloud Sync Active",
                                    color = if (isGuest) PastelLavender else PastelSand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section: Theme & Appearance
                SettingsSection(title = "THEME & PALETTE") {
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Material You Dynamic Colors",
                        subtitle = "Harmonize wallet accents with your Android system wallpaper",
                        checked = useMaterialDynamicTheme,
                        onCheckedChange = { viewModel.setUseMaterialDynamicTheme(it) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Accent Color Palette",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Select a muted pastel theme for buttons and wallet UI elements",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val palettes = listOf(
                        Triple("DEFAULT", "Warm Sand", PastelSand),
                        Triple("MINT", "Emerald Mint", PastelMint),
                        Triple("LAVENDER", "Cyber Lavender", PastelLavender),
                        Triple("ROSE", "Sunset Rose", PastelRose),
                        Triple("SLATE", "Ocean Slate", PastelSlate)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        palettes.forEach { (key, name, color) ->
                            val isSelected = selectedPaletteTheme == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) color else DarkBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setSelectedPaletteTheme(key) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name.split(" ").last(),
                                        color = if (isSelected) color else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Tactile & Motion Experience
                SettingsSection(title = "TACTILE & MOTION") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Tactile clicks during card swipes, category flipping and focus",
                        checked = hapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.TouchApp,
                        title = "3D Card Tilt & Parallax",
                        subtitle = "Fluid physical tilting on cards using responsive gestures",
                        checked = cardTilt3dEnabled,
                        onCheckedChange = { viewModel.setCardTilt3dEnabled(it) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Layers,
                        title = "Compact Stack Density",
                        subtitle = "Reduce vertical card overlap spacing for faster scrolling",
                        checked = compactStackView,
                        onCheckedChange = { viewModel.setCompactStackView(it) }
                    )
                }

                // Section: Intelligent Features & Location
                SettingsSection(title = "INTELLIGENCE & LOCATION") {
                    SettingsSwitchRow(
                        icon = Icons.Default.CloudOff,
                        title = "On-Device Local Model",
                        subtitle = "Fast, private offline semantic search without sending data to cloud",
                        checked = useLocalAiModel,
                        onCheckedChange = { viewModel.setUseLocalAiModel(it) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Gemini Flash-Lite OCR",
                        subtitle = "Intelligent multimodal correction for clean field parsing",
                        checked = autoGeminiOcr,
                        onCheckedChange = { viewModel.setAutoGeminiOcr(it) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.NearMe,
                        title = "Show Travel Distance & ETA",
                        subtitle = "Calculate live driving distance and transit time to card addresses",
                        checked = showDistanceEta,
                        onCheckedChange = { viewModel.setShowDistanceEta(it) }
                    )
                }

                // Section: Wallet Maintenance & Local Storage
                SettingsSection(title = "WALLET DATA & BACKUP") {
                    SettingsActionRow(
                        icon = Icons.Default.CreditCard,
                        title = "Wallet Inventory",
                        subtitle = "${filteredCards.size} cards currently in wallet",
                        actionLabel = "View"
                    ) {
                        onBack()
                    }
                    SettingsActionRow(
                        icon = Icons.Default.Security,
                        title = "Clean & Deduplicate Cards",
                        subtitle = "Scan wallet for duplicate phone, email, or names and merge them",
                        actionLabel = "Clean"
                    ) {
                        viewModel.deduplicateEntireWallet { count ->
                            if (count > 0) {
                                Toast.makeText(context, "Cleaned $count duplicate card(s)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No duplicates found in wallet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    SettingsActionRow(
                        icon = Icons.Default.FileUpload,
                        title = "Export Wallet (JSON)",
                        subtitle = "Back up all business cards to JSON text or shareable file",
                        actionLabel = "Export"
                    ) {
                        exportJsonContent = viewModel.exportWalletJson()
                        showExportDialog = true
                    }
                    SettingsActionRow(
                        icon = Icons.Default.FileDownload,
                        title = "Import Wallet (JSON)",
                        subtitle = "Restore or merge cards from a JSON backup file",
                        actionLabel = "Import"
                    ) {
                        importInputText = ""
                        showImportDialog = true
                    }

                }

                // Log Out / Exit Guest Session Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PastelRose.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutDialog = true }
                        .testTag("btn_settings_logout")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (isGuest) "Exit Guest Mode" else "Log Out",
                            tint = PastelRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGuest) "Exit Guest Session" else "Sign Out",
                                color = PastelRose,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGuest) "Switch to Google Sign-In or exit guest wallet" else "Disconnect wallet session from this device",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Text(
                        text = "Export Wallet (JSON)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Here is your complete wallet data in JSON format. You can copy it or share it as a backup file:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = exportJsonContent,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Wallet JSON", exportJsonContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Wallet JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelMint,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copy JSON", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, exportJsonContent)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Wallet JSON")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelLavender,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text(
                        text = "Import Wallet (JSON)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Paste a JSON export snippet below to restore or add cards to your wallet:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = { importInputText = it },
                            placeholder = { Text("Paste JSON here...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PastelLavender,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importInputText.isNotBlank()) {
                                viewModel.importWalletJson(importInputText) { count ->
                                    if (count > 0) {
                                        Toast.makeText(context, "Successfully imported $count cards!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                    } else {
                                        Toast.makeText(context, "Invalid JSON format or 0 cards found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelMint,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Import Cards", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Sign Out / Exit Guest Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = {
                    Text(
                        text = if (isGuest) "Exit Guest Session?" else "Sign out of Wallet?",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isGuest) {
                            "You are currently in local guest mode. If you have not exported a JSON backup, ensure you export your cards before exiting."
                        } else {
                            "Your business cards are securely backed up in the cloud and will be restored when you sign in again."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelRose,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isGuest) "Exit Session" else "Log Out", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
