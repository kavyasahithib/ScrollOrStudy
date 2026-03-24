package com.example.scrollorstudy.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scrollorstudy.ScrollOrStudyApplication
import com.example.scrollorstudy.data.local.PreferencesManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ParentDashboardUiState(
    val studentStudyTime: Long = 0L,
    val studentScrollTime: Long = 0L,
    val studentStreak: Int = 0,
    val studentName: String = "Loading...",
    val weeklyChartUrl: String? = null
)

class ParentDashboardViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://scrollorstudy-default-rtdb.asia-southeast1.firebasedatabase.app")
    
    private val _uiState = MutableStateFlow(ParentDashboardUiState())
    val uiState: StateFlow<ParentDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.studentUidForParent.collect { uid ->
                if (uid.isNotEmpty()) {
                    observeStudentData(uid)
                }
            }
        }
    }

    private fun observeStudentData(studentUid: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        database.getReference("user_data").child(studentUid).child(currentDate)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val study = snapshot.child("studyTime").getValue(Long::class.java) ?: 0L
                    val scroll = snapshot.child("scrollTime").getValue(Long::class.java) ?: 0L
                    _uiState.value = _uiState.value.copy(studentStudyTime = study, studentScrollTime = scroll)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        database.getReference("user_stats").child(studentUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val streak = snapshot.child("streak").getValue(Int::class.java) ?: 0
                    val name = snapshot.child("userName").getValue(String::class.java) ?: "Student"
                    _uiState.value = _uiState.value.copy(studentStreak = streak, studentName = name)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        database.getReference("weekly_reports").child(studentUid).child("latest_chart_url")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val url = snapshot.getValue(String::class.java)
                    _uiState.value = _uiState.value.copy(weeklyChartUrl = url)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ScrollOrStudyApplication)
                ParentDashboardViewModel(preferencesManager = application.container.preferencesManager)
            }
        }
    }
}
