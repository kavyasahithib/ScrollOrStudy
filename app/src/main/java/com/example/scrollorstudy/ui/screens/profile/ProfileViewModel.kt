package com.example.scrollorstudy.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scrollorstudy.ScrollOrStudyApplication
import com.example.scrollorstudy.data.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "student",
    val isDarkMode: Boolean = false,
    val userId: String = ""
)

class ProfileViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    val uiState: StateFlow<ProfileUiState> = combine(
        preferencesManager.userName,
        preferencesManager.userRole,
        preferencesManager.isDarkMode
    ) { name, role, darkMode ->
        ProfileUiState(
            userName = name,
            userEmail = currentUser?.email ?: "",
            userRole = role,
            isDarkMode = darkMode,
            userId = currentUser?.uid ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun updateName(name: String) = viewModelScope.launch { preferencesManager.setUserName(name) }
    fun toggleDarkMode(isDark: Boolean) = viewModelScope.launch { preferencesManager.setDarkMode(isDark) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ScrollOrStudyApplication)
                ProfileViewModel(preferencesManager = application.container.preferencesManager)
            }
        }
    }
}
