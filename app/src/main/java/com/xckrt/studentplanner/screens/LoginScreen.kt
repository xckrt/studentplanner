package com.xckrt.studentplanner.screens


import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckrt.studentplanner.MainActivity
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.viewmodels.AuthViewModel


@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    apiService: ApiService,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGroupSelection: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Вход в систему",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(apiService) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                modifier = Modifier,
                color = Color.White,
                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                trackColor = ProgressIndicatorDefaults.circularTrackColor,
                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
            } else {
                Text("ВОЙТИ")
            }
        }
        viewModel.errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { onNavigateToRegister() }) {
            Text("Нет аккаунта? Зарегистрироваться", color = Color.Cyan)
        }
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val activity = context as? Activity
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
    if (viewModel.isLoginSuccessful) {
        LaunchedEffect(Unit) {

            Log.d("LoginFlow", "LaunchedEffect сработал. UI видит группу: ${viewModel.loggedInGroupId}")

            if (viewModel.loggedInGroupId > 0) {
                Log.d("LoginFlow", "Решение: Летим на РАСПИСАНИЕ!")
                onNavigateToSchedule()
            } else {
                Log.d("LoginFlow", "Решение: Летим на ВЫБОР ГРУППЫ!")
                onNavigateToGroupSelection()
            }
        }
    }
}