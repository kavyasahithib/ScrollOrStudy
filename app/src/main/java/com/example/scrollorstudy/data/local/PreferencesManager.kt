package com.example.scrollorstudy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scroll_study_prefs")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore

    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[IS_DARK_MODE] ?: false }
    val isHardcoreModeActive: Flow<Boolean> = dataStore.data.map { it[IS_HARDCORE_MODE] ?: false }
    val isStudyModeActive: Flow<Boolean> = dataStore.data.map { it[IS_STUDY_MODE] ?: false }
    val studyTimeToday: Flow<Long> = dataStore.data.map { it[STUDY_TIME] ?: 0L }
    val scrollTimeToday: Flow<Long> = dataStore.data.map { it[SCROLL_TIME] ?: 0L }
    val currentStreak: Flow<Int> = dataStore.data.map { it[STREAK] ?: 0 }
    val userRole: Flow<String> = dataStore.data.map { it[USER_ROLE] ?: "student" }
    val userName: Flow<String> = dataStore.data.map { it[USER_NAME] ?: "" }
    val studentUidForParent: Flow<String> = dataStore.data.map { it[STUDENT_UID] ?: "" }
    val lastSyncStatus: Flow<String> = dataStore.data.map { it[LAST_SYNC_STATUS] ?: "Ready" }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { it[IS_DARK_MODE] = isDark }
    }
    
    suspend fun setHardcoreMode(isActive: Boolean) {
        dataStore.edit { it[IS_HARDCORE_MODE] = isActive }
    }

    suspend fun setStudyMode(isActive: Boolean) {
        dataStore.edit { it[IS_STUDY_MODE] = isActive }
    }

    suspend fun updateTime(studyDelta: Long, scrollDelta: Long) {
        dataStore.edit { prefs ->
            prefs[STUDY_TIME] = (prefs[STUDY_TIME] ?: 0L) + studyDelta
            prefs[SCROLL_TIME] = (prefs[SCROLL_TIME] ?: 0L) + scrollDelta
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun setUserRole(role: String) {
        dataStore.edit { it[USER_ROLE] = role }
    }

    suspend fun setStudentUidForParent(uid: String) {
        dataStore.edit { it[STUDENT_UID] = uid }
    }

    suspend fun resetDailyStats() {
        dataStore.edit { prefs ->
            prefs[STUDY_TIME] = 0L
            prefs[SCROLL_TIME] = 0L
        }
    }

    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_HARDCORE_MODE = booleanPreferencesKey("is_hardcore_mode_active")
        val IS_STUDY_MODE = booleanPreferencesKey("is_study_mode_active")
        val STUDY_TIME = longPreferencesKey("study_time")
        val SCROLL_TIME = longPreferencesKey("scroll_time")
        val STREAK = intPreferencesKey("current_streak")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_NAME = stringPreferencesKey("user_name")
        val STUDENT_UID = stringPreferencesKey("student_uid")
        val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
    }
}
