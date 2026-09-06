package com.example.proyecto_evacuapp.utils

import android.content.Context
import android.media.MediaPlayer
import com.example.proyecto_evacuapp.R

object CustomVoicePlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(context: Context, audioResId: Int) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(context, audioResId)?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun getAudioForStep(modifier: String): Int {
        return when (modifier.lowercase().trim()) {
            "slight left" -> R.raw.gira_leve_izquierda
            "left" -> R.raw.gira_izquierda
            "sharp left" -> R.raw.gira_fuerte_izquierda

            "slight right" -> R.raw.gira_leve_derecha
            "right" -> R.raw.gira_derecha
            "sharp right" -> R.raw.gira_fuerte_derecha

            "uturn" -> R.raw.vuelta_u
            "depart" -> R.raw.inicio_evacuacion
            else -> R.raw.continua_recto
        }
    }
}