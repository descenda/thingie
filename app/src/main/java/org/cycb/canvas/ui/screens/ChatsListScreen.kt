@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.cycb.canvas.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ripple
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cycb.canvas.data.model.Chat
import org.cycb.canvas.ui.components.ProfilePicture
import org.cycb.canvas.ui.components.UnreadBadge
import org.cycb.canvas.viewmodel.ChatsUiState
import org.cycb.canvas.viewmodel.ChatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatsListScreen(
    viewModel: ChatsViewModel,
    onChatClick: (String) -> Unit,
    onCreateChatClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onDemoClick: () -> Unit = {},
    userProfilePicture: String? = null,
    userDisplayName: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pinnedChatIds by viewModel.pinnedChatIds.collectAsState()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }

    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadChats()
            kotlinx.coroutines.delay(1000)
            isRefreshing = false
        }
    }

    if (showOptionsSheet && selectedChatId != null) {
        ChatOptionsSheet(
            chatId = selectedChatId!!,
            isPinned = pinnedChatIds.contains(selectedChatId),
            onDismiss = { showOptionsSheet = false },
            onPinClick = {
                viewModel.togglePinChat(selectedChatId!!)
                showOptionsSheet = false
            },
            onDeleteClick = {
                viewModel.hideChat(selectedChatId!!)
                showOptionsSheet = false
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ChatsTopAppBar(
                onDiscoverClick = onDiscoverClick,
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
                userProfilePicture = userProfilePicture,
                userDisplayName = userDisplayName,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                isRefreshing = true
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ChatsUiState.Loading -> {
                    LoadingState()
                }
                is ChatsUiState.Success -> {
                    val chats = state.chats
                    if (chats.isEmpty()) {
                        EmptyState(
                            onCreateChatClick = onCreateChatClick,
                            isSearching = searchQuery.isNotBlank()
                        )
                    } else {
                        ChatsList(
                            chats = chats,
                            pinnedChatIds = pinnedChatIds,
                            onChatClick = onChatClick,
                            onChatLongClick = { chatId ->
                                selectedChatId = chatId
                                showOptionsSheet = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            listState = listState
                        )
                    }
                }
                is ChatsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadChats() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsTopAppBar(
    onDiscoverClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userProfilePicture: String? = null,
    userDisplayName: String = "",
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = {
            Text(
                "Chats",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Search")
            }
            IconButton(onClick = onDiscoverClick) {
                Icon(Icons.Default.Explore, "Discover")
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                ProfilePicture(
                    imageUrl = userProfilePicture,
                    displayName = userDisplayName,
                    size = 36.dp
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@Composable
private fun ChatsList(
    chats: List<Chat>,
    pinnedChatIds: Set<String>,
    onChatClick: (String) -> Unit,
    onChatLongClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = chats,
            key = { it.getChatId() }
        ) { chat ->
            ChatListItem(
                chat = chat,
                isPinned = pinnedChatIds.contains(chat.getChatId()),
                onClick = { onChatClick(chat.getChatId()) },
                onLongClick = { onChatLongClick(chat.getChatId()) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: Chat,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            ),
        color = if (chat.unreadCount > 0) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) 
        else 
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                ProfilePicture(
                    imageUrl = if (chat.type == "group") chat.avatar else chat.otherUser?.profilePicture,
                    displayName = chat.name ?: chat.otherUser?.displayName ?: "Unknown",
                    size = 56.dp
                )
                if (chat.otherUser?.isOnline == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(14.dp).rotate(45f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = chat.name ?: chat.otherUser?.displayName ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatTimestamp(chat.lastMessage?.timestamp ?: chat.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.lastMessage?.content ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                    )
                    if (chat.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        UnreadBadge(count = chat.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator()
    }
}

@Composable
private fun EmptyState(onCreateChatClick: () -> Unit, isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(if (isSearching) "No results" else "No chats yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (!isSearching) {
                Button(onClick = onCreateChatClick) {
                    Text("Start a conversation")
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(timestamp) ?: return timestamp
        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { time = date }
        if (now.get(Calendar.DATE) == msgTime.get(Calendar.DATE)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) { timestamp }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOptionsSheet(chatId: String, isPinned: Boolean, onDismiss: () -> Unit, onPinClick: () -> Unit, onDeleteClick: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text(if (isPinned) "Unpin" else "Pin") },
                leadingContent = { Icon(Icons.Default.PushPin, null) },
                modifier = Modifier.clickable { onPinClick() }
            )
            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDeleteClick() }
            )
        }
    }
}
