package com.example.proyecto_evacuapp

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.UserMeResponse
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EvacuApp : Application() {
    override fun onCreate() {
        super.onCreate()

        RetrofitClient.init(this)

        val database = EvacuAppDatabase.getInstance(this)
        IncidentSharedState.initialize(database)

        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        if (!token.isNullOrEmpty()) {
            val userId = prefs.getString("user_id", "") ?: ""
            val userName = prefs.getString("user_name", "") ?: ""
            val userEmail = prefs.getString("user_email", "") ?: ""
            val userRole = prefs.getString("user_role", "") ?: ""

            if (userId.isNotBlank()) {
                UserSessionState.currentUser = UserSessionState.currentUser.copy(
                    id = userId,
                    name = userName,
                    email = userEmail,
                    role = userRole,
                    isLoggedIn = true
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val meResponse = RetrofitClient.userApiService.getMe()
                        if (meResponse.isSuccessful && meResponse.body() != null) {
                            UserSessionState.updateFromUserMeResponse(meResponse.body()!!)
                        }
                    } catch (e: Exception) {
                        Log.w("EvacuApp", "getMe on startup failed, using cached data", e)
                    }
                }
            }
        }
    }
}