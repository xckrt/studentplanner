package com.xckrt.studentplanner

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.data.AuthManager
import com.xckrt.studentplanner.db.AppDatabase
import com.xckrt.studentplanner.notifications.AlarmScheduler
import com.xckrt.studentplanner.screens.*
import com.xckrt.studentplanner.viewmodels.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(apiService: ApiService, tokenManager: TokenManager) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val noteDao = remember { AppDatabase.getDatabase(context).noteDao()}
    val taskDao = remember { AppDatabase.getDatabase(context).taskDao() }
    val alarmScheduler = remember { AlarmScheduler(context) }
    val tutorialStep by tokenManager.tutorialStep.collectAsState(initial = 1)
    val isFirstLaunch by tokenManager.isFirstLaunch.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val savedToken by tokenManager.token.collectAsState(initial = null)
    val savedGroupId by tokenManager.groupId.collectAsState(initial = 0)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(savedToken) {
        AuthManager.token = savedToken
    }

    val showBottomBar = currentRoute != "login" && currentRoute != "register" && currentRoute != "group_selection"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    MyBottomNavigation(navController, currentRoute, tokenManager)
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = if (savedToken != null) {
                    if (savedGroupId == null || savedGroupId == 0) "group_selection" else "schedule"
                } else "login",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("login") {
                    val authVM: AuthViewModel = viewModel(factory = AuthViewModelFactory(tokenManager))
                    LoginScreen(
                        viewModel = authVM,
                        apiService = apiService,
                        onNavigateToSchedule = {
                            navController.navigate("schedule") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToGroupSelection = {
                            navController.navigate("group_selection") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToRegister = { navController.navigate("register") }
                    )
                }
                composable("schedule") {
                    val database = AppDatabase.getDatabase(context)
                    val scheduleVM: ScheduleViewModel = viewModel(
                        factory = ScheduleViewModelFactory(database.noteDao(), database.taskDao(), tokenManager)
                    )
                    ScheduleScreen(scheduleVM, groupId = savedGroupId ?: 0, noteDao = noteDao,tokenManager)
                }
                composable("tasks") {
                    val taskVM: TaskViewModel = viewModel(
                        factory = TaskViewModelFactory(taskDao, noteDao, apiService, tokenManager, alarmScheduler)
                    )
                    TaskScreen(viewModel = taskVM,tokenManager)
                }
                composable("register") {
                    val regVM: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(tokenManager))
                    RegisterScreen(
                        viewModel = regVM,
                        apiService = apiService,
                        onSuccess = { navController.navigate("login") },
                        onBackToLogin = { navController.popBackStack() }
                    )
                }
                composable("group_selection") {
                    val factory = remember { GroupSelectionViewModelFactory(apiService, tokenManager, context.applicationContext as Application) }
                    val selectionVM: GroupSelectionViewModel = viewModel(factory = factory)
                    GroupSelectionScreen(viewModel = selectionVM, onGroupSelected = { navController.navigate("schedule") { popUpTo("group_selection") { inclusive = true } } })
                }
                composable("profile") {
                    ProfileScreen(tokenManager = tokenManager, onLogout = { navController.navigate("login") { popUpTo(0) { inclusive = true } } }, onNavigateToSelection = { navController.navigate("group_selection") { popUpTo("profile") { inclusive = true } } })
                }
            }
        }
        val isKofiInDialog by tokenManager.isKofiInDialog.collectAsState()
        if (isFirstLaunch && tutorialStep <= 12 && (currentRoute == "schedule" || currentRoute == "tasks") && !isKofiInDialog) {
            GlobalKofiTutorial(
                step = tutorialStep,
                onNext = {
                    scope.launch { tokenManager.saveTutorialStep(tutorialStep + 1) }
                },
                onFinish = {
                    scope.launch {
                        tokenManager.saveTutorialStep(13)
                        tokenManager.setFirstLaunchCompleted()
                    }
                }
            )
        }
    }
}

@Composable
fun MyBottomNavigation(
    navController: NavHostController,
    currentRoute: String?,
    tokenManager: TokenManager
) {
    val tutorialStep by tokenManager.tutorialStep.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "schedule",
            onClick = {
                navController.navigate("schedule") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text("Пары") }
        )
        NavigationBarItem(
            selected = currentRoute == "tasks",
            onClick = {
                if (tutorialStep == 5) {
                    scope.launch {
                        tokenManager.saveTutorialStep(6)
                    }
                }
                navController.navigate("tasks") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.AutoMirrored.Default.List, contentDescription = null) },
            label = { Text("Задачи") }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = {
                navController.navigate("profile") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Профиль") }
        )
    }
}
