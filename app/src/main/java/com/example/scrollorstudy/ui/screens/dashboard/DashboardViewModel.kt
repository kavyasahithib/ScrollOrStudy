package com.example.scrollorstudy.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scrollorstudy.ScrollOrStudyApplication
import com.example.scrollorstudy.data.local.PreferencesManager
import com.example.scrollorstudy.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val studyTimeToday: Long = 0L,
    val scrollTimeToday: Long = 0L,
    val currentStreak: Int = 0,
    val aiMessage: String = "Analyzing patterns...",
    val aiCoachMessage: String = "Awaiting AI Coach analysis...",
    val focusRank: String = "Unranked",
    val focusScore: Int = 0,
    val userName: String = "Student",
    val isHardcoreModeActive: Boolean = false,
    val isStudyModeActive: Boolean = false
)

class DashboardViewModel(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            preferencesManager.studyTimeToday,
            preferencesManager.scrollTimeToday,
            preferencesManager.currentStreak,
            preferencesManager.userName,
            preferencesManager.isHardcoreModeActive
        ) { study, scroll, streak, name, hardcore ->
            DashboardUiState(
                studyTimeToday = study,
                scrollTimeToday = scroll,
                currentStreak = streak,
                userName = name,
                isHardcoreModeActive = hardcore
            )
        },
        combine(
            userRepository.getAiInsights(),
            userRepository.getAiMotivation(),
            userRepository.getLeaderboardStats(),
            preferencesManager.isStudyModeActive
        ) { insights, motivation, leaderboard, studyMode ->
            DashboardUiState(
                aiMessage = insights,
                aiCoachMessage = motivation,
                focusRank = leaderboard.rank,
                focusScore = leaderboard.score,
                isStudyModeActive = studyMode
            )
        }
    ) { state1, state2 ->
        state1.copy(
            aiMessage = state2.aiMessage,
            aiCoachMessage = state2.aiCoachMessage,
            focusRank = state2.focusRank,
            focusScore = state2.focusScore,
            isStudyModeActive = state2.isStudyModeActive
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = DashboardUiState())

    fun toggleHardcoreMode(isActive: Boolean) = viewModelScope.launch { preferencesManager.setHardcoreMode(isActive) }
    
    fun toggleStudyMode() = viewModelScope.launch {
        val current = preferencesManager.isStudyModeActive.first()
        preferencesManager.setStudyMode(!current)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ScrollOrStudyApplication)
                DashboardViewModel(
                    preferencesManager = application.container.preferencesManager,
                    userRepository = application.container.userRepository
                )
            }
        }
    }
}
