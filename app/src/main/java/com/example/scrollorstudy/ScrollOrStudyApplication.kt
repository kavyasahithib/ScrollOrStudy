package com.example.scrollorstudy

import android.app.Application
import com.example.scrollorstudy.di.AppContainer
import com.example.scrollorstudy.di.DefaultAppContainer

class ScrollOrStudyApplication : Application() {
    lateinit var container: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
