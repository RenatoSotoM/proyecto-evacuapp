package com.example.proyecto_evacuapp

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EvacuApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicialización de base de datos local, estado compartido y motor de ruteo
        val database = EvacuAppDatabase.getInstance(this)
        IncidentSharedState.initialize(database)
        com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.initialize(applicationContext)

        // Inicialización del Módulo de Comunicaciones Offline de Producción (SMS & Bluetooth RFCOMM)
        com.example.proyecto_evacuapp.comm.EmergencyCommManager.initialize(
            applicationContext,
            CoroutineScope(Dispatchers.IO)
        )

        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)

        if (!token.isNullOrEmpty()) {
            // 🔑 PASO CLAVE: Asignar el token a Retrofit ANTES de cualquier llamada a la API
            RetrofitClient.authToken = token

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
            }

            // Petición asíncrona para actualizar los datos del usuario desde PostgreSQL al arrancar
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val meResponse = RetrofitClient.userApiService.getMe()
                    if (meResponse.isSuccessful && meResponse.body() != null) {
                        UserSessionState.updateFromUserMeResponse(meResponse.body()!!)
                    }
                } catch (e: Exception) {
                    Log.w("EvacuApp", "getMe en inicio falló, usando datos en caché local", e)
                }
            }
        }
    }
}
