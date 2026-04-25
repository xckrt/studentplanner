package com.xckrt.studentplanner.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.data.GroupItem
import com.xckrt.studentplanner.data.UserRegisterRequest
import kotlinx.coroutines.launch

class RegisterViewModel(private val tokenManager: TokenManager) : ViewModel() {
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var groups by mutableStateOf<List<GroupItem>>(emptyList())
    var selectedGroup by mutableStateOf<GroupItem?>(null)

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isSuccess by mutableStateOf(false)

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")


    fun register(apiService: ApiService) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = UserRegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                )
                val response = apiService.register(request)
                if (response.isSuccessful) {

                    isSuccess = true
                } else {
                    errorMessage = "Ошибка: этот Email уже занят или данные неверны"
                }
            } catch (e: Exception) {
                errorMessage = "Проблема с сетью: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
class RegisterViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}