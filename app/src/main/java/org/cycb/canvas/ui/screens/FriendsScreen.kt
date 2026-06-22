@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.cycb.canvas.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cycb.canvas.data.model.User
import org.cycb.canvas.viewmodel.FriendsUiState
import org.cycb.canvas.viewmodel.FriendsViewModel

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onFriendClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    userProfilePicture: String? = null,
    userDisplayName: String = "",
    isSidebar: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!isSidebar) {
                TopAppBar(
                    title = {
                        Text(
                            "People",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onProfileClick) {
                            org.cycb.canvas.ui.components.ProfilePicture(
                                imageUrl = userProfilePicture,
                                displayName = userDisplayName,
                                size = 32.dp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddFriendDialog = true }) {
                            Icon(Icons.Default.PersonAdd, "Add Friend")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search people...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            when (val state = uiState) {
                is FriendsUiState.Success -> {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Friends", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                BadgedBox(
                                    badge = {
                                        if (state.friendRequests.isNotEmpty()) {
                                            Badge { Text(state.friendRequests.size.toString()) }
                                        }
                                    }
                                ) {
                                    Text("Requests", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> FriendsList(
                                friends = state.friends.filter {
                                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                                            it.username.contains(searchQuery, ignoreCase = true)
                                },
                                onFriendClick = onFriendClick,
                                onMessageClick = onMessageClick,
                                onRemoveFriend = { viewModel.removeFriend(it) },
                                isSidebar = isSidebar
                            )
                            1 -> FriendRequestsList(
                                requests = state.friendRequests.filter {
                                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                                            it.username.contains(searchQuery, ignoreCase = true)
                                },
                                onAccept = { viewModel.acceptFriendRequest(it) },
                                onDecline = { viewModel.declineFriendRequest(it) },
                                isSidebar = isSidebar
                            )
                        }
                    }
                }
                is FriendsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator()
                    }
                }
                is FriendsUiState.Error -> {
                    ErrorState(message = state.message, onRetry = { viewModel.loadFriends() })
                }
            }
        }

        if (showAddFriendDialog) {
            AddFriendDialog(
                onDismiss = { showAddFriendDialog = false },
                onAddFriend = { username ->
                    viewModel.sendFriendRequest(username)
                    showAddFriendDialog = false
                }
            )
        }
    }
}

@Composable
fun FriendsList(
    friends: List<User>,
    onFriendClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    isSidebar: Boolean = false
) {
    if (friends.isEmpty()) {
        EmptyState(icon = Icons.Default.PersonOff, message = "No friends found")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = if (isSidebar) 4.dp else 8.dp, bottom = if (isSidebar) 16.dp else 120.dp)
        ) {
            itemsIndexed(friends) { _, friend ->
                FriendListItem(
                    friend = friend,
                    onClick = { onFriendClick(friend.getUserId()) },
                    onMessageClick = { onMessageClick(friend.getUserId()) },
                    onRemoveClick = { onRemoveFriend(friend.getUserId()) }
                )
            }
        }
    }
}

@Composable
fun FriendListItem(
    friend: User,
    onClick: () -> Unit,
    onMessageClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(friend.displayName, fontWeight = FontWeight.Bold) },
        supportingContent = { Text("@${friend.username}") },
        leadingContent = {
            Box {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = friend.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (friend.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(1.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onMessageClick) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onRemoveClick) {
                    Icon(Icons.Outlined.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun FriendRequestsList(
    requests: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    isSidebar: Boolean = false
) {
    if (requests.isEmpty()) {
        EmptyState(icon = Icons.Default.CheckCircle, message = "No pending requests")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = if (isSidebar) 4.dp else 8.dp, bottom = if (isSidebar) 16.dp else 120.dp)
        ) {
            itemsIndexed(requests) { _, request ->
                FriendRequestListItem(
                    user = request,
                    onAccept = { onAccept(request.getUserId()) },
                    onDecline = { onDecline(request.getUserId()) }
                )
            }
        }
    }
}

@Composable
fun FriendRequestListItem(
    user: User,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    ListItem(
        headlineContent = { Text(user.displayName, fontWeight = FontWeight.Bold) },
        supportingContent = { Text("@${user.username}") },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.displayName.firstOrNull()?.uppercase() ?: "?")
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, shape = RoundedCornerShape(12.dp)) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onDecline, shape = RoundedCornerShape(12.dp)) {
                    Text("Ignore")
                }
            }
        }
    )
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚠️", style = MaterialTheme.typography.displayMedium)
            Text(message, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun AddFriendDialog(onDismiss: () -> Unit, onAddFriend: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("username") },
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onAddFriend(username) }, enabled = username.isNotBlank()) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
