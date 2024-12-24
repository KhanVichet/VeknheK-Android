package com.example.final_project

import android.app.Application
import com.example.final_project.utility.PreferencesUtil
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferencesUtil.init(this)
        FirebaseApp.initializeApp(this)
    }
}