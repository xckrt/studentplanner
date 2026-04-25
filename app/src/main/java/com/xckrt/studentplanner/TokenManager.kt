package com.xckrt.studentplanner

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xckrt.studentplanner.TokenManager.Companion.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
private val Context.dataStore by preferencesDataStore(name = "settings")
class TokenManager(private val context: Context) {



    companion object {
        private val Context.preferencesDataStore by preferencesDataStore("user_prefs")
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val GROUP_ID_KEY = intPreferencesKey("group_id")

        private val TUTORIAL_STEP_KEY = intPreferencesKey("tutorial_step")
        val USER_ID_KEY = intPreferencesKey("user_id")

        val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
        val SILENT_MODE_KEY = booleanPreferencesKey("silent_mode")
        private val FIRST_NAME_KEY = stringPreferencesKey("first_name")
        private val LAST_NAME_KEY = stringPreferencesKey("last_name")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("is_first_launch")] ?: true
    }
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("is_first_launch")] = false
        }
    }
    suspend fun saveAuthData(token: String, groupId: Int, userId: Int) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[GROUP_ID_KEY] = groupId
            it[USER_ID_KEY] = userId
        }
    }
    suspend fun saveGroupId(groupId: Int) {
        context.dataStore.edit { preferences ->
            preferences[GROUP_ID_KEY] = groupId
        }
    }
    suspend fun setSilentMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENT_MODE_KEY] = enabled
        }
    }
    suspend fun setDarkMode(isDark: Boolean) { context.dataStore.edit { it[DARK_MODE_KEY] = isDark } }
    suspend fun saveProfileData(fName: String, lName: String, avatar: String) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_NAME_KEY] = fName
            preferences[LAST_NAME_KEY] = lName
            preferences[AVATAR_URL_KEY] = avatar
        }
    }
    suspend fun saveTutorialStep(step: Int) {
        Log.d("Tutorial", "DataStore ЗАПИСЬ: $step")
        context.dataStore.edit { prefs ->
            prefs[TUTORIAL_STEP_KEY] = step
        }
    }
    fun setKofiInDialog(visible: Boolean) {
        _isKofiInDialog.value = visible
    }
    suspend fun logout() { context.dataStore.edit { it.clear() } }
    private val _isKofiInDialog = MutableStateFlow(false)
    val isKofiInDialog: StateFlow<Boolean> = _isKofiInDialog.asStateFlow()
    val tutorialStep: Flow<Int> = context.dataStore.data.map { it[TUTORIAL_STEP_KEY] ?: 1 }
    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val groupId: Flow<Int?> = context.dataStore.data.map { it[GROUP_ID_KEY] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[USER_ID_KEY] }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_KEY] ?: true }
    val isSilentModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[SILENT_MODE_KEY] ?: false }
    val firstName: Flow<String?> = context.dataStore.data.map { it[FIRST_NAME_KEY] }
    val lastName: Flow<String?> = context.dataStore.data.map { it[LAST_NAME_KEY] }
    val avatarUrl: Flow<String?> = context.dataStore.data.map { it[AVATAR_URL_KEY] }
}