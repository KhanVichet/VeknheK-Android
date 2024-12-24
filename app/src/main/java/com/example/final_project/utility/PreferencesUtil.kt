package com.example.final_project.utility

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtil {
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences("veknhek", Context.MODE_PRIVATE)
    }

    fun putToken(string: String) = preferences.edit().putString("TOKEN", string).apply()
    fun putUserId(id: Long) = preferences.edit().putLong("USER_ID", id).apply()

    fun getToken(): String = preferences.getString("TOKEN", "") ?: ""
    fun getUserId(): Long = preferences.getLong("USER_ID", 0)

    fun alreadyLogin(): Boolean = getToken().isNotEmpty() && getUserId() != 0L
    fun clearNecessary() = preferences.edit().remove("TOKEN").apply()
}