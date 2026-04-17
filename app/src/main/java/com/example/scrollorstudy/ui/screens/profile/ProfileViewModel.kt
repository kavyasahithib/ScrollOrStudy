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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "student",
    val isDarkMode: Boolean = false,
    val userId: String = "",
    val studyTime: Long = 0L,
    val streak: Int = 0
)

class ProfileViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.userName,
                preferencesManager.userRole,
                preferencesManager.isDarkMode,
                preferencesManager.studentUidForParent
            ) { name, role, darkMode, studentUid ->
                listOf(name, role, darkMode.toString(), studentUid)
            }.collect { data ->
                val role = data[1]
                val darkMode = data[2].toBoolean()
                val studentUid = data[3]
                
                if (role == "student") {
                    _uiState.value = _uiState.value.copy(
                        userRole = role,
                        isDarkMode = darkMode,
                        userId = currentUser?.uid ?: ""
                    )
                    fetchStudentProfile(currentUser?.uid ?: "")
                } else if (role == "parent" && studentUid.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        userRole = role,
                        isDarkMode = darkMode,
                        userId = studentUid
                    )
                    fetchStudentProfile(studentUid)
                }
            }
        }
    }

    private fun fetchStudentProfile(uid: String) {
        if (uid.isEmpty()) return
        val db = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
        db.getReference("user_stats").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sName = snapshot.child("userName").getValue(String::class.java) ?: "Student"
                val sEmail = snapshot.child("userEmail").getValue(String::class.java) ?: "No Email Info"
                val streak = snapshot.child("streak").getValue(Int::class.java) ?: 0
                _uiState.value = _uiState.value.copy(
                    userName = sName, 
                    userEmail = sEmail,
                    streak = streak
                )
                
                // Keep local preference completely in-sync if they are a student
                if (_uiState.value.userRole == "student") {
                    viewModelScope.launch { preferencesManager.setUserName(sName) }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateName(name: String) {
        if (_uiState.value.userRole == "student" && _uiState.value.userId.isNotEmpty()) {
            val db = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
            db.getReference("user_stats").child(_uiState.value.userId).child("userName").setValue(name)
        }
    }

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
