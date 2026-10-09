package com.example.proyecto_evacuapp.comm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import com.example.proyecto_evacuapp.utils.IncidentNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val body = sms.messageBody ?: continue
            val payload = EmergencyPayload.parseFromSmsFrame(body)
            if (payload != null) {
                Log.d("SmsReceiver", "🚨 Trama SMS de emergencia recibida de ${sms.originatingAddress}: ${payload.type}")

                val typeEnum = IncidentType.fromApiValue(payload.type)
                val newIncident = SharedIncident(
                    localId = payload.localId,
                    type = typeEnum,
                    severity = IncidentSeverity.ALTA,
                    description = "[SMS Terreno] ${payload.description}",
                    latitude = payload.latitude,
                    longitude = payload.longitude,
                    createdAtMillis = payload.createdAtMillis,
                    status = IncidentStatus.VERIFIED,
                    isOwnReport = false
                )

                CoroutineScope(Dispatchers.IO).launch {
                    IncidentSharedState.addLocalIncident(newIncident)
                    IncidentNotificationManager.checkAndNotifyProximityIncidents(
                        context = context,
                        userLocation = com.example.proyecto_evacuapp.ui.components.UserLocationState.currentLocation,
                        incidents = listOf(newIncident)
                    )
                }
            }
        }
    }
}
