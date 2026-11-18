package com.example.proyecto_evacuapp.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logger de métricas estructurado en formato CSV optimizado para Power BI.
 * Registra ejecuciones de ruteo, consensos e inferencias.
 */
object PowerBiCsvLogger {

    private const val TAG = "POWERBI_CSV"
    private const val FILE_NAME = "metricas_evacuapp.csv"
    private const val HEADER = "Timestamp,Ejecucion_ID,Modo,Perfil,Tipo_Ruta,Destino_Nombre,Destino_Coords,Nodos_Procesados,Aristas_Procesadas,Carga_BBox_ms,Calculo_Ruta_ms,Tiempo_Total_ms,Distancia_m,Tiempo_Estimado_min,Giros,RAM_Usada_MB,Inferencia_ONNX_ms,Trama_Bytes,Exito"

    fun logMetricRow(
        context: Context?,
        ejecucionId: String,
        modo: String = "Offline",
        perfil: String,
        tipoRuta: String,
        destinoNombre: String = "N/A",
        destinoCoords: String = "N/A",
        nodosProcesados: Int,
        aristasProcesadas: Int,
        cargaBBoxMs: Long,
        calculoRutaMs: Long,
        tiempoTotalMs: Long,
        distanciaMeters: Double,
        tiempoEstimadoMin: Int,
        giros: Int,
        inferenciaOnnxMs: Double = 0.0,
        tramaBytes: Int = 0,
        exito: Boolean = true
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = sdf.format(Date())

        val runtime = Runtime.getRuntime()
        val usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        val ramUsedFormatted = String.format(Locale.US, "%.2f", usedRamMb)
        val distanciaFormatted = String.format(Locale.US, "%.1f", distanciaMeters)
        val inferenciaOnnxFormatted = String.format(Locale.US, "%.3f", inferenciaOnnxMs)

        // Limpiar cualquier coma en destinoNombre y formatear coordenadas con ';' para no romper el formato CSV
        val cleanDestinoNombre = destinoNombre.replace(",", " ").trim().ifBlank { "N/A" }
        val cleanDestinoCoords = destinoCoords.replace(",", ";").trim().ifBlank { "N/A" }

        val csvRow = "$timestamp,$ejecucionId,$modo,$perfil,$tipoRuta,$cleanDestinoNombre,$cleanDestinoCoords,$nodosProcesados,$aristasProcesadas,$cargaBBoxMs,$calculoRutaMs,$tiempoTotalMs,$distanciaFormatted,$tiempoEstimadoMin,$giros,$ramUsedFormatted,$inferenciaOnnxFormatted,$tramaBytes,${if (exito) "TRUE" else "FALSE"}"

        // 1. Imprime en Logcat usando exclusivamente el tag POWERBI_CSV (solo el valor CSV puro)
        Log.d(TAG, csvRow)

        // 2. Guarda acumulativamente en context.getExternalFilesDir(null)/metricas_evacuapp.csv
        if (context != null) {
            try {
                val dir = context.getExternalFilesDir(null)
                if (dir != null) {
                    val csvFile = File(dir, FILE_NAME)
                    val isNew = !csvFile.exists() || csvFile.length() == 0L
                    FileWriter(csvFile, true).use { writer ->
                        if (isNew) {
                            writer.append(HEADER).append("\n")
                        }
                        writer.append(csvRow).append("\n")
                    }
                }
            } catch (e: Exception) {
                Log.e("POWERBI_CSV_ERR", "Error registrando fila CSV: ${e.message}")
            }
        }
    }
}
