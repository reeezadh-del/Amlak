package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.UiState
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.CreateEditListingScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.theme.BiVaseteTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_EXPLORE = "explore"
const val ROUTE_CHATS = "chats"
const val ROUTE_PROFILE = "profile"
const val ROUTE_CREATE = "create"
const val ROUTE_DETAIL = "detail/{propertyId}"
const val ROUTE_EDIT = "edit/{propertyId}"
const val ROUTE_CHAT_DETAIL = "chat/{conversationId}"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val topDestinations = listOf(
    TopLevelDestination(
        route = ROUTE_EXPLORE,
        label = "آگهی‌ها",
        selectedIcon = Icons.Filled.HomeWork,
        unselectedIcon = Icons.Outlined.HomeWork,
        testTag = "nav_explore",
    ),
    TopLevelDestination(
        route = ROUTE_CHATS,
        label = "گفتگوها",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        testTag = "nav_chats",
    ),
    TopLevelDestination(
        route = ROUTE_PROFILE,
        label = "حساب من",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiVaseteTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "کاربر بی‌واسطه"
        val email = user.email

        key(uid) {
            val viewModel: MarketplaceViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MarketplaceViewModel(
                            repository = MarketplaceRepository(db),
                            currentUserId = uid,
                            currentUserName = displayName,
                        )
                    }
                }
            )

            BiVaseteAppShell(
                viewModel = viewModel,
                displayName = displayName,
                email = email,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiVaseteAppShell(
    viewModel: MarketplaceViewModel,
    displayName: String,
    email: String?,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val activePropertiesState by viewModel.filteredPropertiesState.collectAsStateWithLifecycle()
    val myPropertiesState by viewModel.myPropertiesState.collectAsStateWithLifecycle()
    val savedPropertiesState by viewModel.savedPropertiesState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val isTopLevel = currentRoute in topDestinations.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (isTopLevel) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    ROUTE_CHATS -> "گفتگوهای مستقیم"
                                    ROUTE_PROFILE -> "پروفایل و آگهی‌های من"
                                    else -> "بی‌واسطه"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            },
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding(),
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_EXPLORE || currentRoute == ROUTE_PROFILE) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(ROUTE_CREATE) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("ثبت آگهی رایگان") },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("fab_create_listing"),
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(
                        modifier = Modifier.statusBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_EXPLORE,
                    ) {
                        composable(ROUTE_EXPLORE) {
                            ExploreScreen(
                                propertiesState = activePropertiesState,
                                searchQuery = searchQuery,
                                selectedType = selectedType,
                                sortOption = sortOption,
                                favoriteIds = favoriteIds,
                                currentUserId = viewModel.currentUserId,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onFilterTypeSelected = viewModel::onFilterTypeSelected,
                                onSortOptionSelected = viewModel::onSortOptionSelected,
                                onPropertyClick = { id -> navController.navigate("detail/$id") },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onCreateClick = { navController.navigate(ROUTE_CREATE) },
                            )
                        }

                        composable(ROUTE_CHATS) {
                            ChatsScreen(
                                conversationsState = conversationsState,
                                currentUserId = viewModel.currentUserId,
                                onConversationClick = { convId -> navController.navigate("chat/$convId") },
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                displayName = displayName,
                                email = email,
                                currentUserId = viewModel.currentUserId,
                                myPropertiesState = myPropertiesState,
                                savedPropertiesState = savedPropertiesState,
                                favoriteIds = favoriteIds,
                                onPropertyClick = { id -> navController.navigate("detail/$id") },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onCreateClick = { navController.navigate(ROUTE_CREATE) },
                                onSignOutClick = onSignOut,
                            )
                        }

                        composable(ROUTE_CREATE) {
                            CreateEditListingScreen(
                                existingProperty = null,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onSubmit = { title, desc, type, city, neighborhood, price, area, beds, year, floor, parking, elevator, storage, deed, preset, photo ->
                                    viewModel.createProperty(
                                        title = title,
                                        description = desc,
                                        propertyType = type,
                                        city = city,
                                        neighborhood = neighborhood,
                                        priceToman = price,
                                        areaSqm = area,
                                        bedrooms = beds,
                                        buildYear = year,
                                        floor = floor,
                                        hasParking = parking,
                                        hasElevator = elevator,
                                        hasStorage = storage,
                                        hasTitleDeed = deed,
                                        coverPreset = preset,
                                        photoBase64 = photo,
                                        onSuccess = { newId ->
                                            navController.popBackStack()
                                            navController.navigate("detail/$newId")
                                        },
                                    )
                                },
                            )
                        }

                        composable(
                            route = ROUTE_DETAIL,
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
                        ) { entry ->
                            val propertyId = entry.arguments?.getString("propertyId") ?: return@composable
                            val detailFlow = remember(propertyId) { viewModel.observePropertyDetail(propertyId) }
                            val propertyState by detailFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            PropertyDetailScreen(
                                propertyState = propertyState,
                                isFavorite = propertyId in favoriteIds,
                                currentUserId = viewModel.currentUserId,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onStartChat = { prop ->
                                    viewModel.openDirectChat(prop) { convId ->
                                        navController.navigate("chat/$convId")
                                    }
                                },
                                onEditClick = { id -> navController.navigate("edit/$id") },
                                onMarkSold = { id -> viewModel.markAsSold(id) },
                                onDelete = { id ->
                                    viewModel.deleteProperty(id) {
                                        navController.popBackStack()
                                    }
                                },
                            )
                        }

                        composable(
                            route = ROUTE_EDIT,
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
                        ) { entry ->
                            val propertyId = entry.arguments?.getString("propertyId") ?: return@composable
                            val detailFlow = remember(propertyId) { viewModel.observePropertyDetail(propertyId) }
                            val propertyState by detailFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                            val existing = (propertyState as? UiState.Success)?.data

                            if (existing != null) {
                                CreateEditListingScreen(
                                    existingProperty = existing,
                                    isBusy = isBusy,
                                    onBack = { navController.popBackStack() },
                                    onSubmit = { title, desc, type, city, neighborhood, price, area, beds, year, floor, parking, elevator, storage, deed, preset, photo ->
                                        viewModel.updateProperty(
                                            propertyId = propertyId,
                                            title = title,
                                            description = desc,
                                            propertyType = type,
                                            city = city,
                                            neighborhood = neighborhood,
                                            priceToman = price,
                                            areaSqm = area,
                                            bedrooms = beds,
                                            buildYear = year,
                                            floor = floor,
                                            hasParking = parking,
                                            hasElevator = elevator,
                                            hasStorage = storage,
                                            hasTitleDeed = deed,
                                            coverPreset = preset,
                                            photoBase64 = photo,
                                            onSuccess = { navController.popBackStack() },
                                        )
                                    },
                                )
                            }
                        }

                        composable(
                            route = ROUTE_CHAT_DETAIL,
                            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                        ) { entry ->
                            val convId = entry.arguments?.getString("conversationId") ?: return@composable
                            val convFlow = remember(convId) { viewModel.observeConversationDetail(convId) }
                            val msgsFlow = remember(convId) { viewModel.observeChatMessages(convId) }
                            val convState by convFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                            val msgsState by msgsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            ChatDetailScreen(
                                conversationState = convState,
                                messagesState = msgsState,
                                currentUserId = viewModel.currentUserId,
                                onBack = { navController.popBackStack() },
                                onOpenProperty = { propId -> navController.navigate("detail/$propId") },
                                onSendMessage = { conv, text, onSent ->
                                    viewModel.sendChatMessage(conv, text, onSent)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
