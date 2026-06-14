package com.xckrt.studentplanner.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigationevent.NavigationEvent
import com.xckrt.studentplanner.TokenManager
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.data.GroupItem
import com.xckrt.studentplanner.data.UpdateGroupRequest
import com.xckrt.studentplanner.service.FcmTopics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class GroupSelectionViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val application: Application
) : ViewModel() {

    var groups by mutableStateOf<List<GroupItem>>(emptyList())
    var selectedGroup by mutableStateOf<GroupItem?>(null)

    var isUploading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchGroups()
    }

    private fun fetchGroups() {
        viewModelScope.launch {
            try {
                groups = apiService.getGroups()
            } catch (e: Exception) {
                errorMessage = "Не удалось загрузить список групп"
            }
        }
    }
    fun selectGroup(groupId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.updateGroup(UpdateGroupRequest(groupId))

                if (response.isSuccessful) {
                    val oldGroupId = tokenManager.groupId.first()
                    tokenManager.saveGroupId(groupId)
                    FcmTopics.switchGroup(newGroupId = groupId, oldGroupId = oldGroupId)
                    onComplete()
                } else {
                    errorMessage = "Ошибка сервера: не удалось привязать группу"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка сети: ${e.message}"
            }
        }
    }


    fun uploadExcel(uri: Uri) {
        viewModelScope.launch {
            isUploading = true
            errorMessage = null
            try {
                val filePart = prepareFilePart(uri)
                val response = apiService.uploadSchedule(filePart)

                if (response.isSuccessful) {
                    fetchGroups()
                    errorMessage = "Файл обработан. Выберите вашу группу из списка выше."
                } else {
                    errorMessage = "Ошибка парсинга. Проверьте формат файла."
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка соединения: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }
    private fun prepareFilePart(uri: Uri): MultipartBody.Part {
        val inputStream = application.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".xls", application.cacheDir)
        tempFile.outputStream().use { inputStream?.copyTo(it) }

        val requestFile = tempFile.asRequestBody("application/vnd.ms-excel".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
    }
}
class GroupSelectionViewModelFactory(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GroupSelectionViewModel(apiService, tokenManager, application) as T
    }
}