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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    userDisplayName: String = "",
    isSidebar: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pinnedChatIds by viewModel.pinnedChatIds.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedFolderId by remember { mutableStateOf<String?>(null) } // null means "All Chats"

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
            folders = folders,
            onDismiss = { showOptionsSheet = false },
            onPinClick = {
                viewModel.togglePinChat(selectedChatId!!)
                showOptionsSheet = false
            },
            onDeleteClick = {
                viewModel.hideChat(selectedChatId!!)
                showOptionsSheet = false
            },
            onAddToFolderClick = { folderId ->
                val folder = folders.find { it.id == folderId }
                if (folder != null) {
                    viewModel.updateFolder(folder.copy(chatIds = folder.chatIds + selectedChatId!!))
                }
                showOptionsSheet = false
            },
            onCreateFolderClick = { name ->
                viewModel.createFolder(name, setOf(selectedChatId!!))
                showOptionsSheet = false
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isSidebar) {
                Column {
                    ChatsTopAppBar(
                        onDiscoverClick = onDiscoverClick,
                        onSearchClick = onSearchClick,
                        onProfileClick = onProfileClick,
                        userProfilePicture = userProfilePicture,
                        userDisplayName = userDisplayName,
                        scrollBehavior = scrollBehavior
                    )
                    
                    if (folders.isNotEmpty()) {
                        SecondaryScrollableTabRow(
                            selectedTabIndex = if (selectedFolderId == null) 0 else folders.indexOfFirst { it.id == selectedFolderId } + 1,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedFolderId == null,
                                onClick = { selectedFolderId = null },
                                text = { Text("All Chats") }
                            )
                            folders.forEach { folder ->
                                Tab(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { selectedFolderId = folder.id },
                                    text = { Text(folder.name) }
                                )
                            }
                        }
                    }
                }
            }
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
                    val allChats = state.chats
                    val filteredChats = if (selectedFolderId == null) {
                        allChats
                    } else {
                        val folder = folders.find { it.id == selectedFolderId }
                        allChats.filter { folder?.chatIds?.contains(it.getChatId()) == true }
                    }

                    if (filteredChats.isEmpty()) {
                        EmptyState(
                            onCreateChatClick = onCreateChatClick,
                            isSearching = searchQuery.isNotBlank() || selectedFolderId != null
                        )
                    } else {
                        ChatsList(
                            chats = filteredChats,
                            pinnedChatIds = pinnedChatIds,
                            onChatClick = onChatClick,
                            onChatLongClick = { chatId ->
                                selectedChatId = chatId
                                showOptionsSheet = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            listState = listState,
                            isSidebar = isSidebar
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
    TopAppBar(
        title = {
            Text(
                "Chats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                ProfilePicture(
                    imageUrl = userProfilePicture,
                    displayName = userDisplayName,
                    size = 32.dp
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Search")
            }
            IconButton(onClick = onDiscoverClick) {
                Icon(Icons.Default.Explore, "Discover")
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
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
    listState: androidx.compose.foundation.lazy.LazyListState,
    isSidebar: Boolean = false
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = if (isSidebar) 4.dp else 8.dp, bottom = if (isSidebar) 16.dp else 100.dp),
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
    val chatName = chat.name ?: chat.otherUser?.displayName ?: "Unknown"
    val lastMsg = chat.lastMessage?.content ?: "No messages yet"
    val unreadText = if (chat.unreadCount > 0) ", ${chat.unreadCount} unread messages" else ""
    val pinnedText = if (isPinned) ", pinned" else ""
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Chat with $chatName, $lastMsg$unreadText$pinnedText"
                role = Role.Button
            }
            .combinedClickable(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
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
fun ChatOptionsSheet(
    chatId: String,
    isPinned: Boolean,
    folders: List<org.cycb.canvas.data.model.ChatFolder>,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddToFolderClick: (String) -> Unit = {},
    onCreateFolderClick: (String) -> Unit = {}
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            onCreateFolderClick(folderName)
                            showCreateFolderDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text(if (isPinned) "Unpin" else "Pin") },
                leadingContent = { Icon(Icons.Default.PushPin, null) },
                modifier = Modifier.clickable { onPinClick() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Text(
                "Folders",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            folders.forEach { folder ->
                val isInFolder = folder.chatIds.contains(chatId)
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = { Icon(Icons.Default.Folder, null) },
                    trailingContent = {
                        if (isInFolder) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable { onAddToFolderClick(folder.id) }
                )
            }

            ListItem(
                headlineContent = { Text("New Folder...") },
                leadingContent = { Icon(Icons.Default.CreateNewFolder, null) },
                modifier = Modifier.clickable { showCreateFolderDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            ListItem(
                headlineContent = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDeleteClick() }
            )
        }
    }
}
