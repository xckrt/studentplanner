package com.xckrt.studentplanner

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xckrt.studentplanner.RetrofitClient.apiService
import com.xckrt.studentplanner.screens.AnimatedSplashScreen
import com.xckrt.studentplanner.ui.theme.StudentPlannerTheme
import com.xckrt.studentplanner.utils.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val audioWorkRequest = PeriodicWorkRequestBuilder<com.xckrt.studentplanner.utils.ScheduleAudioWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ScheduleAudioWork",
            ExistingPeriodicWorkPolicy.KEEP,
            audioWorkRequest
        )
        val taskSyncRequest = PeriodicWorkRequestBuilder<com.xckrt.studentplanner.utils.TaskSyncWorker>(
            6, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            taskSyncRequest
        )
        scheduleDailyReminder()
        setContent {
            val tokenManager = remember { TokenManager(applicationContext) }
            val isDarkTheme by tokenManager.isDarkMode.collectAsState(initial = true)
            var isSplashFinished by remember { mutableStateOf(false) }

            StudentPlannerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isSplashFinished) {
                        AnimatedSplashScreen(
                            onAnimationFinished = {
                                isSplashFinished = true
                            }
                        )
                    } else {
                        RequestNotificationPermission()
                        AppNavigation(apiService, tokenManager)
                    }
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        AppForeground.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.isForeground = false
    }

    private fun scheduleDailyReminder() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}


@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
            } else {
            }
        }
        LaunchedEffect(key1 = true) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}