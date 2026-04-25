package com.xckrt.studentplanner.screens

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.utils.CalendarManager
import com.xckrt.studentplanner.utils.FileUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onNavigateToSelection: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }


    val isDarkMode by tokenManager.isDarkMode.collectAsState(initial = true)
    val groupId by tokenManager.groupId.collectAsState(initial = 0)
    val isSilentModeEnabled by tokenManager.isSilentModeEnabled.collectAsState(initial = false)
    val savedFirstName by tokenManager.firstName.collectAsState(initial = "")
    val savedLastName by tokenManager.lastName.collectAsState(initial = "")
    val savedAvatarUrl by tokenManager.avatarUrl.collectAsState(initial = "")


    var firstName by remember(savedFirstName) { mutableStateOf(savedFirstName ?: "") }
    var lastName by remember(savedLastName) { mutableStateOf(savedLastName ?: "") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val BASE_URL = "http://5.42.114.104:5262"


    val audioController = remember { com.xckrt.studentplanner.utils.AudioController(context) }
    val calendarManager = remember { com.xckrt.studentplanner.utils.CalendarManager(context) }
    var hasAudioPermission by remember { mutableStateOf(audioController.hasPermission()) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> profileImageUri = uri }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            scope.launch {
                try {
                    val currentGroupId = tokenManager.groupId.first() ?: 0
                    if (currentGroupId == 0) {
                        snackbarHostState.showSnackbar("Ошибка: группа не выбрана")
                        return@launch
                    }
                    snackbarHostState.showSnackbar("Загрузка расписания...")
                    val allLessons = apiService.getSchedule(currentGroupId)
                    var successCount = 0
                    allLessons.forEach { lesson ->
                        if (calendarManager.addLessonToCalendar(lesson)) successCount++
                    }
                    snackbarHostState.showSnackbar("Синхронизировано: $successCount пар")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Ошибка сети: ${e.localizedMessage}")
                }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Нужны права на работу с календарем") }
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = audioController.hasPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мой Профиль", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)) },
                actions = {
                    IconButton(onClick = {
                        isEditMode = !isEditMode
                        if (!isEditMode) profileImageUri = null
                    }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = isEditMode) { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(model = profileImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (!savedAvatarUrl.isNullOrBlank()) {
                        AsyncImage(model = "$BASE_URL$savedAvatarUrl", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    if (isEditMode) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
            if (isEditMode) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSavingProfile = true
                                try {
                                    val fNameBody = firstName.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val lNameBody = lastName.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())

                                    var avatarPart: MultipartBody.Part? = null
                                    if (profileImageUri != null) {
                                        val file = FileUtils.getFileFromUri(context, profileImageUri!!)
                                        if (file != null) {
                                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                            avatarPart = MultipartBody.Part.createFormData("Avatar", file.name, requestFile)
                                        }
                                    }

                                    val response = apiService.updateProfile(fNameBody, lNameBody, avatarPart)
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        tokenManager.saveProfileData(
                                            fName = body?.firstName ?: "",
                                            lName = body?.lastName ?: "",
                                            avatar = body?.avatarUrl ?: ""
                                        )
                                        isEditMode = false
                                        snackbarHostState.showSnackbar("Профиль обновлен")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                } finally {
                                    isSavingProfile = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingProfile) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("Сохранить изменения")
                    }
                }
            } else {
                Text(
                    text = if (savedFirstName.isNullOrBlank()) "Студент" else "$savedFirstName $savedLastName",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Группа №$groupId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))


            SettingsSection(title = "Обучение") {
                SettingsRow(
                    icon = Icons.Default.Person,
                    title = "Моя группа",
                    subtitle = if (groupId == 0) "Не выбрана" else "Группа №$groupId",
                    onClick = { onNavigateToSelection() }
                )
            }

            SettingsSection(title = "Инструменты") {
                SettingsRow(
                    icon = Icons.Default.DateRange,
                    title = "Google Календарь",
                    subtitle = "Синхронизировать пары",
                    onClick = {
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    }
                )
                SettingsSwitchRow(
                    icon = Icons.Default.NotificationsOff,
                    title = "Тихий режим",
                    subtitle = if (!hasAudioPermission) "Нужны права системы" else "Авто-беззвучный на парах",
                    checked = isSilentModeEnabled && hasAudioPermission,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (hasAudioPermission) {
                                scope.launch { tokenManager.setSilentMode(true) }
                            } else {
                                audioController.requestPermission()
                            }
                        } else {
                            scope.launch { tokenManager.setSilentMode(false) }
                        }
                    }
                )
            }

            SettingsSection(title = "Интерфейс") {
                SettingsSwitchRow(
                    icon = Icons.Default.Person,
                    title = "Темная тема",
                    subtitle = "Экономия заряда и глаз",
                    checked = isDarkMode,
                    onCheckedChange = { isChecked ->
                        scope.launch { tokenManager.setDarkMode(isChecked) }
                    }
                )
            }

            // --- ВЫХОД ---
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = { scope.launch { tokenManager.logout(); onLogout() } },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}