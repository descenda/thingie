package org.cycb.canvas.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.cycb.canvas.ui.auth.LoginScreen
import org.cycb.canvas.ui.auth.SignUpScreen
import org.cycb.canvas.ui.screens.*
import org.cycb.canvas.viewmodel.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CYCBApp(
    initialRoute: String? = null,
    chatId: String? = null,
    userId: String? = null
) {
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(context) }

    val user by authViewModel.user.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(initialRoute, chatId, userId, user) {
        if (user != null && initialRoute != null) {
            when (initialRoute) {
                "chat" -> chatId?.let { navController.navigate("chat/$it") }
                "friends" -> navController.navigate("friends")
                "chats" -> navController.navigate("chats")
                "profile" -> userId?.let { navController.navigate("profile/$it") }
            }
        }
    }

    LaunchedEffect(Unit) {
        org.cycb.canvas.data.api.ApiConfig.initialize()
    }

    val chatsViewModel = remember(user) { ChatsViewModel(context.applicationContext as android.app.Application) }
    val chatRoomViewModel = remember(user) { ChatRoomViewModel() }
    val friendsViewModel = remember(user) { FriendsViewModel() }
    val publicChatsViewModel = remember(user) { org.cycb.canvas.viewmodel.PublicChatsViewModel() }
    val voiceCallViewModel = remember(user) { org.cycb.canvas.viewmodel.VoiceCallViewModel(context.applicationContext as android.app.Application) }

    val currentCall by voiceCallViewModel.currentCall.collectAsState()
    val callDuration by voiceCallViewModel.callDuration.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentChatId = currentBackStackEntry?.arguments?.getString("chatId")

    val showBottomBar = currentRoute == "home"
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )

    val currentHomeTab = when (pagerState.currentPage) {
        0 -> "dashboard"
        1 -> "chats"
        2 -> "friends"
        else -> "dashboard"
    }
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(modifier = Modifier.size(48.dp))
        }
        return
    }

    val startDestination = if (user == null) "login" else "home"

    Scaffold(
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToSignUp = { navController.navigate("signup") }
                    )
                }
                composable("signup") {
                    SignUpScreen(
                        viewModel = authViewModel,
                        onSignUpSuccess = {
                            navController.navigate("home") { popUpTo("signup") { inclusive = true } }
                        },
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }
                composable("home") {
                    MainScreensWithSwipe(
                        pagerState = pagerState,
                        user = user,
                        chatsViewModel = chatsViewModel,
                        friendsViewModel = friendsViewModel,
                        navController = navController
                    )
                }
                composable("create_group") {
                    CreateGroupChatScreen(
                        viewModel = remember { CreateGroupViewModel() },
                        onBackClick = { navController.popBackStack() },
                        onGroupCreated = { groupId ->
                            navController.navigate("chat/$groupId") {
                                popUpTo("chats") { inclusive = false }
                            }
                        }
                    )
                }
                composable("public_chats") {
                    PublicChatsScreen(
                        viewModel = publicChatsViewModel,
                        onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("expressive_demo") {
                    ExpressiveDemoScreen(onBackClick = { navController.popBackStack() })
                }
                composable(
                    route = "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                    ChatRoomScreen(
                        chatId = chatId,
                        chat = null,
                        viewModel = chatRoomViewModel,
                        voiceCallViewModel = voiceCallViewModel,
                        currentUserId = user?.getUserId() ?: "",
                        currentUsername = user?.displayName ?: user?.username ?: "",
                        onBackClick = { navController.popBackStack() },
                        onProfileClick = { uid -> navController.navigate("profile/$uid") },
                        onGroupInfoClick = { navController.navigate("group_info/$chatId") },
                        onChatBackgroundClick = { navController.navigate("chat_background/$chatId") }
                    )
                }
                composable(
                    route = "profile/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    ProfileScreen(
                        userId = userId,
                        user = if (userId == user?.id) user else null,
                        isOwnProfile = userId == user?.id,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToChat = { cid -> navController.navigate("chat/$cid") },
                        onEditClick = { navController.navigate("edit_profile") },
                        onSettingsClick = { navController.navigate("settings") },
                        onAddFriendClick = {}
                    )
                }
                composable("edit_profile") {
                    EditProfileScreen(
                        user = user ?: return@composable,
                        onSaveClick = { authViewModel.refreshUser(); navController.popBackStack() },
                        onCancelClick = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate("login") { popUpTo("home") { inclusive = true } }
                        },
                        onNavigateToThemePicker = { navController.navigate("theme_picker") },
                        onNavigateToUpdate = { navController.navigate("update_screen") }
                    )
                }
                composable("theme_picker") {
                    ThemePickerScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToCustomTheme = { navController.navigate("custom_theme_creator") }
                    )
                }
                composable("custom_theme_creator") {
                    CustomThemeCreatorScreen(onBackClick = { navController.popBackStack() }, onThemeCreated = { navController.popBackStack() })
                }
                composable("update_screen") {
                    val updateInfo = org.cycb.canvas.utils.UpdateManager.cachedUpdateInfo
                    if (updateInfo != null) {
                        UpdateScreen(updateInfo = updateInfo, onBackClick = { navController.popBackStack() })
                    } else {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
                composable("search_users") {
                    SearchUsersScreen(onBackClick = { navController.popBackStack() }, onUserClick = { uid -> navController.navigate("profile/$uid") })
                }
                
                composable(
                    route = "group_info/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                    val groupInfoViewModel = remember { org.cycb.canvas.viewmodel.GroupInfoViewModel() }
                    val groupInfoState by groupInfoViewModel.uiState.collectAsState()
                    LaunchedEffect(chatId) { groupInfoViewModel.loadGroupInfo(chatId) }
                    
                    when (val state = groupInfoState) {
                        is org.cycb.canvas.viewmodel.GroupInfoUiState.Success -> {
                            GroupInfoScreen(
                                chat = org.cycb.canvas.data.model.Chat(id = state.chatInfo.id, type = state.chatInfo.type, name = state.chatInfo.name),
                                members = state.members,
                                currentUserId = user?.getUserId() ?: "",
                                onBackClick = { navController.popBackStack() },
                                onMemberClick = { uid -> navController.navigate("profile/$uid") },
                                onLeaveGroup = { groupInfoViewModel.leaveGroup(chatId) { navController.navigate("chats") } },
                                onEditGroup = { navController.navigate("edit_group/$chatId") },
                                onAddMembers = { navController.navigate("add_members/$chatId") },
                                onPromoteToAdmin = { mid -> groupInfoViewModel.updateMemberRole(chatId, mid, "admin") },
                                onDemoteFromAdmin = { mid -> groupInfoViewModel.updateMemberRole(chatId, mid, "member") },
                                onRemoveMember = { mid -> groupInfoViewModel.removeMember(chatId, mid) },
                                onChatBackgroundClick = { navController.navigate("chat_background/$chatId") }
                            )
                        }
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    }
                }
            }

            // Google Chat style Floating Navigation Bar
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding()
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    when (pagerState.currentPage) {
                                        0 -> scope.launch { pagerState.animateScrollToPage(1) }
                                        1 -> navController.navigate("create_group")
                                        2 -> navController.navigate("search_users")
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                Icon(Icons.Default.Add, "Action")
                            }
                        },
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(64.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FloatingToolbarIconButton(
                                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                icon = if (pagerState.currentPage == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home",
                                isSelected = pagerState.currentPage == 0
                            )
                            FloatingToolbarIconButton(
                                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                icon = if (pagerState.currentPage == 1) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Chats",
                                isSelected = pagerState.currentPage == 1
                            )
                            FloatingToolbarIconButton(
                                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                icon = if (pagerState.currentPage == 2) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "People",
                                isSelected = pagerState.currentPage == 2
                            )
                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                            FloatingToolbarIconButton(
                                onClick = { navController.navigate("settings") },
                                icon = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                isSelected = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingToolbarIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreensWithSwipe(
    pagerState: androidx.compose.foundation.pager.PagerState,
    user: org.cycb.canvas.data.model.User?,
    chatsViewModel: ChatsViewModel,
    friendsViewModel: FriendsViewModel,
    navController: androidx.navigation.NavHostController
) {
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                val dashboardViewModel: org.cycb.canvas.viewmodel.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val currentUserSummary = user?.let {
                    org.cycb.canvas.data.model.UserSummary(it.getUserId(), it.username, it.displayName, it.profilePicture)
                }

                DashboardScreen(
                    viewModel = dashboardViewModel,
                    chatsViewModel = chatsViewModel,
                    currentUser = currentUserSummary,
                    onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                    onNoteClick = { userId ->
                        chatsViewModel.getOrCreatePrivateChat(
                            userId = userId,
                            onSuccess = { chatId -> navController.navigate("chat/$chatId") },
                            onError = { }
                        )
                    },
                    onUserClick = { userId -> navController.navigate("profile/$userId") },
                    onSearchClick = { navController.navigate("search_users") },
                    onSettingsClick = { navController.navigate("settings") },
                    onNewGroupClick = { navController.navigate("create_group") },
                    onPublicChatsClick = { navController.navigate("public_chats") },
                    onMoreClick = { navController.navigate("settings") }
                )
            }
            1 -> {
                ChatsListScreen(
                    viewModel = chatsViewModel,
                    onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                    onCreateChatClick = { navController.navigate("create_group") },
                    onDiscoverClick = { navController.navigate("public_chats") },
                    onSearchClick = { navController.navigate("search_users") },
                    onDemoClick = { navController.navigate("expressive_demo") },
                    onProfileClick = { user?.id?.let { uid -> navController.navigate("profile/$uid") } },
                    userProfilePicture = user?.profilePicture,
                    userDisplayName = user?.displayName ?: ""
                )
            }
            2 -> {
                LaunchedEffect(Unit) { friendsViewModel.loadFriends() }
                FriendsScreen(
                    viewModel = friendsViewModel,
                    onFriendClick = { friendId -> navController.navigate("profile/$friendId") },
                    onMessageClick = { friendId ->
                        chatsViewModel.getOrCreatePrivateChat(
                            userId = friendId,
                            onSuccess = { chatId -> navController.navigate("chat/$chatId") },
                            onError = { }
                        )
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
