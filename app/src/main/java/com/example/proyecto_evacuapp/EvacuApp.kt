package com.example.proyecto_evacuapp

import android.app.Application
import com.example.proyecto_evacuapp.data.remote.RetrofitClient // <--- Importa el cliente
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState

class EvacuApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializa el cliente Retrofit para que el interceptor tenga acceso al contexto y al token JWT
        RetrofitClient.init(this) // <--- Agrégalo aquí

        // Inicializa la base de datos Room y conecta el estado global de incidentes
        val database = EvacuAppDatabase.getInstance(this)
        IncidentSharedState.initialize(database)
    }
}