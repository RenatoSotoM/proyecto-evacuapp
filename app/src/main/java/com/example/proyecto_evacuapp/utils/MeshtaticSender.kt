package com.example.proyecto_evacuapp.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast

object MeshtaticSender {

    /**
     * Envía un texto mediante un Broadcast o Intent a la App de Meshtatic instalada en el dispositivo.
     */
    fun sendBroadcastSms(context: Context, text: String) {
        try {
            // Intent estándar de comunicación con la app oficial de Meshtatic
            val intent = Intent("com.geeksville.mesh.ACTION_SEND_TEXT").apply {
                putExtra("com.geeksville.mesh.EXTRA_TEXT", text)
                type = "text/plain"
                setPackage("com.geeksville.mesh")
            }

            context.sendBroadcast(intent)
            Toast.makeText(context, "Transmitiendo reporte por Meshtatic...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Si el servicio en segundo plano no responde, abrir selector explícito
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    setPackage("com.geeksville.mesh")
                }
                context.startActivity(sendIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Error: App Meshtatic no encontrada", Toast.LENGTH_LONG).show()
            }
        }
    }
}