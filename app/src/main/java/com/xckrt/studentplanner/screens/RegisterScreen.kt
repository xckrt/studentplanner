package com.xckrt.studentplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Badge // Отличная иконка для фамилии/бейджа
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.viewmodels.RegisterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    apiService: ApiService,
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Регистрация", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = viewModel.firstName,
            onValueChange = { viewModel.firstName = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))


        OutlinedTextField(
            value = viewModel.lastName,
            onValueChange = { viewModel.lastName = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = { Text("Логин (Никнейм)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Скрыть пароль" else "Показать пароль"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.register(apiService) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                    trackColor = ProgressIndicatorDefaults.circularTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )
            } else {
                Text("ЗАРЕГИСТРИРОВАТЬСЯ")
            }
        }

        viewModel.errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 16.dp))
        }

        TextButton(onClick = { onBackToLogin() }) {
            Text("Уже есть аккаунт? Войти", color = Color.Cyan)
        }
    }

    if (viewModel.isSuccess) {
        LaunchedEffect(Unit) { onSuccess() }
    }
}