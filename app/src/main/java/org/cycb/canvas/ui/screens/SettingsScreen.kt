package org.cycb.canvas.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToThemePicker: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    viewModel: org.cycb.canvas.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val messagesNotif by viewModel.messagesNotif.collectAsState()
    val friendRequestsNotif by viewModel.friendRequestsNotif.collectAsState()
    val chatInvitesNotif by viewModel.chatInvitesNotif.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()

    val darkMode by viewModel.darkMode.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val compactMode by viewModel.compactMode.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val amoledMode by viewModel.amoledMode.collectAsState()
    val largeText by viewModel.largeText.collectAsState()

    val readReceipts by viewModel.readReceipts.collectAsState()
    val typingIndicator by viewModel.typingIndicator.collectAsState()
    val lastSeenVisible by viewModel.lastSeenVisible.collectAsState()
    val biometricLock by viewModel.biometricLock.collectAsState()
    val profilePhotoVisible by viewModel.profilePhotoVisible.collectAsState()

    val autoDownloadMedia by viewModel.autoDownloadMedia.collectAsState()
    val autoPlayGifs by viewModel.autoPlayGifs.collectAsState()
    val enterToSend by viewModel.enterToSend.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null) },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // --- Account Category ---
            item { SettingsCategoryHeader("Account") }
            item {
                var showPasswordDialog by remember { mutableStateOf(false) }
                if (showPasswordDialog) {
                    ChangePasswordDialog(
                        onDismiss = { showPasswordDialog = false },
                        onConfirm = { new ->
                            viewModel.updatePassword(
                                newPassword = new,
                                onSuccess = {
                                    showPasswordDialog = false
                                    android.widget.Toast.makeText(context, "Password updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
                SettingsClickableCard(
                    title = "Change Password",
                    subtitle = "Update your account security",
                    icon = Icons.Default.Lock,
                    onClick = { showPasswordDialog = true }
                )
            }

            // --- Appearance Category ---
            item { SettingsCategoryHeader("Appearance") }
            item {
                SettingsSwitchCard(
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    icon = Icons.Default.DarkMode
                )
            }
            item {
                SettingsClickableCard(
                    title = "Color Theme",
                    subtitle = selectedTheme,
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToThemePicker
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Dynamic Colors",
                    subtitle = "Use system color scheme (Android 12+)",
                    checked = dynamicColors,
                    onCheckedChange = { viewModel.setDynamicColors(it) },
                    icon = Icons.Default.Colorize
                )
            }
            if (darkMode) {
                item {
                    SettingsSwitchCard(
                        title = "AMOLED Mode",
                        subtitle = "Pure black background for OLED screens",
                        checked = amoledMode,
                        onCheckedChange = { viewModel.setAmoledMode(it) },
                        icon = Icons.Default.BrightnessLow
                    )
                }
            }
            item {
                SettingsSwitchCard(
                    title = "Compact Mode",
                    subtitle = "Reduce spacing and padding",
                    checked = compactMode,
                    onCheckedChange = { viewModel.setCompactMode(it) },
                    icon = Icons.Default.ViewCompact
                )
            }

            // --- Accessibility Category ---
            item { SettingsCategoryHeader("Accessibility") }
            item {
                SettingsSwitchCard(
                    title = "High Contrast",
                    subtitle = "Improve visibility of text and icons",
                    checked = highContrast,
                    onCheckedChange = { viewModel.setHighContrast(it) },
                    icon = Icons.Default.Contrast
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Large Text",
                    subtitle = "Increase font sizes across the app",
                    checked = largeText,
                    onCheckedChange = { viewModel.setLargeText(it) },
                    icon = Icons.Default.TextFields
                )
            }

            // --- Notifications Category ---
            item { SettingsCategoryHeader("Notifications") }
            item {
                SettingsSwitchCard(
                    title = "All Notifications",
                    subtitle = "Receive push notifications",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    icon = Icons.Default.Notifications
                )
            }
            if (notificationsEnabled) {
                item {
                    SettingsSwitchCard(
                        title = "Messages",
                        subtitle = "New message alerts",
                        checked = messagesNotif,
                        onCheckedChange = { viewModel.setMessagesNotif(it) },
                        icon = Icons.Default.Message
                    )
                }
                item {
                    SettingsSwitchCard(
                        title = "Friend Requests",
                        subtitle = "Friendship activity",
                        checked = friendRequestsNotif,
                        onCheckedChange = { viewModel.setFriendRequestsNotif(it) },
                        icon = Icons.Default.PersonAdd
                    )
                }
                item {
                    SettingsSwitchCard(
                        title = "Group Invites",
                        subtitle = "Group chat invites",
                        checked = chatInvitesNotif,
                        onCheckedChange = { viewModel.setChatInvitesNotif(it) },
                        icon = Icons.Default.GroupAdd
                    )
                }
                item {
                    SettingsSwitchCard(
                        title = "Sound & Vibration",
                        subtitle = "Alert feedback",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it); viewModel.setVibrationEnabled(it) },
                        icon = Icons.Default.VolumeUp
                    )
                }
            }

            // --- Privacy Category ---
            item { SettingsCategoryHeader("Privacy") }
            item {
                SettingsSwitchCard(
                    title = "Read Receipts",
                    subtitle = "Let others know you've read messages",
                    checked = readReceipts,
                    onCheckedChange = { viewModel.setReadReceipts(it) },
                    icon = Icons.Default.DoneAll
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Typing Indicator",
                    subtitle = "Show when you're typing",
                    checked = typingIndicator,
                    onCheckedChange = { viewModel.setTypingIndicator(it) },
                    icon = Icons.Default.Keyboard
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Online Status",
                    subtitle = "Show your active status",
                    checked = lastSeenVisible,
                    onCheckedChange = { viewModel.setLastSeenVisible(it) },
                    icon = Icons.Default.Visibility
                )
            }
            item {
                SettingsSwitchCard(
                    title = "App Lock",
                    subtitle = "Use biometrics to unlock app",
                    checked = biometricLock,
                    onCheckedChange = { viewModel.setBiometricLock(it) },
                    icon = Icons.Default.Fingerprint
                )
            }

            // --- Chat Settings Category ---
            item { SettingsCategoryHeader("Chat Settings") }
            item {
                SettingsSwitchCard(
                    title = "Auto-Download Media",
                    subtitle = "Automatic media retrieval",
                    checked = autoDownloadMedia,
                    onCheckedChange = { viewModel.setAutoDownloadMedia(it) },
                    icon = Icons.Default.Download
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Auto-Play GIFs",
                    subtitle = "Play GIFs automatically",
                    checked = autoPlayGifs,
                    onCheckedChange = { viewModel.setAutoPlayGifs(it) },
                    icon = Icons.Default.Gif
                )
            }
            item {
                SettingsSwitchCard(
                    title = "Enter to Send",
                    subtitle = "Press Enter to send",
                    checked = enterToSend,
                    onCheckedChange = { viewModel.setEnterToSend(it) },
                    icon = Icons.Default.Send
                )
            }

            // --- About Category ---
            item { SettingsCategoryHeader("About") }
            item {
                SettingsClickableCard(
                    title = if (isCheckingUpdate) "Checking..." else "Updates",
                    subtitle = "Version ${org.cycb.canvas.BuildConfig.VERSION_NAME}",
                    icon = Icons.Default.SystemUpdate,
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            viewModel.checkForUpdates(context) { update ->
                                isCheckingUpdate = false
                                if (update != null && update.isUpdateAvailable) {
                                    onNavigateToUpdate()
                                } else {
                                    android.widget.Toast.makeText(context, "You're on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                ExpressiveLogoutButton(onClick = { showLogoutDialog = true })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    val hapticFeedback = LocalHapticFeedback.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    ElevatedCard(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val slideOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "slide"
    )

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = (20.dp * slideOffset))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLogoutButton(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text("Logout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newPassword.length < 8) error = "Too short"
                else if (newPassword != confirmPassword) error = "No match"
                else onConfirm(newPassword)
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
