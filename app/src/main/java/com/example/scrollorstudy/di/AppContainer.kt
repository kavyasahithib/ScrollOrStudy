package com.example.scrollorstudy.di

import android.content.Context
import com.example.scrollorstudy.data.local.PreferencesManager
import com.example.scrollorstudy.data.repository.UserRepository

interface AppContainer {
    val preferencesManager: PreferencesManager
    val userRepository: UserRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }
    
    override val userRepository: UserRepository by lazy {
        UserRepository()
    }
}
