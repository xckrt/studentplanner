package com.xckrt.studentplanner.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.data.AuthManager
import com.xckrt.studentplanner.data.UserLoginRequest
import com.xckrt.studentplanner.service.FcmTopics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(private val tokenManager: TokenManager) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoginSuccessful by mutableStateOf(false)
    var loggedInGroupId by mutableIntStateOf(0)

    fun login(apiService: ApiService) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = apiService.login(UserLoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("LoginFlow", "Успешный ответ от сервера!")
                        Log.d("LoginFlow", "Токен: ${body.token.take(10)}...")
                        Log.d("LoginFlow", "Пришла группа от сервера: ${body.groupId}")
                        Log.d("LoginFlow", "Пришел ID юзера: ${body.userId}")
                        val oldGroupId = tokenManager.groupId.first()
                        AuthManager.token = body.token
                        tokenManager.saveAuthData(
                            token = body.token,
                            groupId = body.groupId,
                            userId = body.userId
                        )
                        FcmTopics.switchGroup(newGroupId = body.groupId, oldGroupId = oldGroupId)

                        tokenManager.saveProfileData(
                            fName = body.firstName ?: "",
                            lName = body.lastName ?: "",
                            avatar = body.avatarUrl ?: ""
                        )

                        loggedInGroupId = body.groupId
                        isLoginSuccessful = true
                    }
                }
                else {
                    errorMessage = "Ошибка: Неверный логин или пароль"
                }
            } catch (e: Exception) {
                errorMessage = "Нет связи с сервером: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
class AuthViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}