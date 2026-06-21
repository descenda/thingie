@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.cycb.canvas.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.cycb.canvas.data.model.UserSummary
import org.cycb.canvas.ui.components.NotesRow
import org.cycb.canvas.ui.components.shimmerEffect
import org.cycb.canvas.viewmodel.DashboardUiState
import org.cycb.canvas.viewmodel.DashboardViewModel
import org.cycb.canvas.viewmodel.ChatsViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    chatsViewModel: ChatsViewModel,
    currentUser: UserSummary?,
    onChatClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onPublicChatsClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Home",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadDashboardData() },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (val state = uiState) {
                    is DashboardUiState.Success -> {
                        // Notes Section
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PaddingTitle("Thoughts")
                                NotesRow(
                                    currentUser = currentUser,
                                    notes = state.notes,
                                    onAddNoteClick = { showNoteDialog = true },
                                    onNoteClick = { note -> onNoteClick(note.userId._id) }
                                )
                            }
                        }

                        // Online Friends Section
                        if (state.onlineFriends.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaddingTitle("Online Now")
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.onlineFriends) { friend ->
                                            OnlineFriendItem(friend, onUserClick)
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Actions - Redesigned as Chips/Buttons for Google Chat feel
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PaddingTitle("Quick Actions")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickActionChip(
                                        text = "Groups",
                                        icon = Icons.Default.Group,
                                        modifier = Modifier.weight(1f),
                                        onClick = onNewGroupClick
                                    )
                                    QuickActionChip(
                                        text = "Discover",
                                        icon = Icons.Default.Explore,
                                        modifier = Modifier.weight(1f),
                                        onClick = onPublicChatsClick
                                    )
                                    QuickActionChip(
                                        text = "Settings",
                                        icon = Icons.Default.Settings,
                                        modifier = Modifier.weight(1f),
                                        onClick = onMoreClick
                                    )
                                }
                            }
                        }

                        // Recent Chats
                        item {
                            PaddingTitle("Recent Chats")
                        }

                        items(state.recentChats.take(5)) { chat ->
                            ChatListItem(
                                chat = chat,
                                isPinned = false,
                                onClick = { onChatClick(chat.getChatId()) },
                                onLongClick = { }
                            )
                        }
                    }
                    is DashboardUiState.Loading -> {
                        items(8) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                    is DashboardUiState.Error -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error: ${state.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteEditDialog(
            content = noteContent,
            onContentChange = { if (it.length <= 60) noteContent = it },
            onDismiss = { showNoteDialog = false },
            onConfirm = {
                viewModel.createNote(noteContent)
                showNoteDialog = false
                noteContent = ""
            }
        )
    }
}

@Composable
fun OnlineFriendItem(friend: org.cycb.canvas.data.model.User, onClick: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val name = friend.displayName ?: friend.username

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name is online"
                role = Role.Button
            }
            .clickable(
                onClickLabel = "Message $name"
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick(friend.getUserId())
            }
    ) {
        Box {
            AsyncImage(
                model = friend.profilePicture ?: "https://ui-avatars.com/api/?name=${friend.username}",
                contentDescription = null, // Handled by Column semantics
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = friend.displayName.split(" ").first(),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuickActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NoteEditDialog(
    content: String,
    onContentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's on your mind?") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Share a thought...") },
                supportingText = { Text("${content.length}/60") },
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = content.isNotBlank()) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaddingTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
