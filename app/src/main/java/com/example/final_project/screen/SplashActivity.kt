package com.example.final_project.screen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.final_project.R
import com.example.final_project.utility.PreferencesUtil

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loggedIn = PreferencesUtil.alreadyLogin()
        Handler(Looper.getMainLooper()).postDelayed({
            val next = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            val intent = Intent(this, next)
            startActivity(intent)
            finish()
        }, 2000)
    }
}