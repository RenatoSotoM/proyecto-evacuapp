package com.example.proyecto_evacuapp.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.proyecto_evacuapp.R

/**
 * Asistente inclusivo de navegación: emite patrones hápticos adaptativos (vibración)
 * e instrucciones de voz con degradación suave si el hardware no posee motor háptico.
 */
object AdaptiveAssistant {

    private const val TAG = "AdaptiveAssistant"

    // Patrones de vibración hápticos [pausa, vibración, pausa, vibración...]
    private val PATTERN_TURN_RIGHT = longArrayOf(0, 150, 100, 150)
    private val PATTERN_TURN_LEFT = longArrayOf(0, 400)
    private val PATTERN_DANGER = longArrayOf(0, 200, 100, 200, 100, 500)

    fun notifyTurnRight(context: Context, customAudioRes: Int? = null) {
        vibratePattern(context, PATTERN_TURN_RIGHT)
        val audioRes = customAudioRes ?: R.raw.inicio_evacuacion
        CustomVoicePlayer.playAudio(context, audioRes)
    }

    fun notifyTurnLeft(context: Context, customAudioRes: Int? = null) {
        vibratePattern(context, PATTERN_TURN_LEFT)
        val audioRes = customAudioRes ?: R.raw.inicio_evacuacion
        CustomVoicePlayer.playAudio(context, audioRes)
    }

    fun notifyDangerOrBlock(context: Context, messageText: String? = null) {
        vibratePattern(context, PATTERN_DANGER)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            } else {
                Log.d(TAG, "Hardware háptico no disponible. Degrada suavemente a asistencia por voz.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al activar motor háptico: ${e.message}. Continuando solo con voz.")
        }
    }
}
