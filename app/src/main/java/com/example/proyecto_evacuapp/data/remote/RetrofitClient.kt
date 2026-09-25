package com.example.proyecto_evacuapp.data.remote

import android.os.Build
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/v1/"
    private const val PHYSICAL_BASE_URL = "http://192.168.100.12:3000/api/v1/"

    /**
     * Permite sobreescribir manualmente la URL base si es necesario.
     */
    var customBaseUrl: String? = null

    /**
     * Detecta si la aplicación se está ejecutando en un emulador de Android Studio.
     */
    val isEmulator: Boolean
        get() = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || (Build.PRODUCT == "google_sdk")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")

    /**
     * URL base dinámica que conmuta entre emulador (10.0.2.2) y dispositivo físico (192.168.100.12).
     */
    val BASE_URL: String
        get() = customBaseUrl ?: if (isEmulator) EMULATOR_BASE_URL else PHYSICAL_BASE_URL

    // Almacenamiento temporal en memoria del Token JWT
    var authToken: String? = null

    /**
     * Instancia permisiva de Gson para manejar respuestas grandes/comprimidas sin lanzar MalformedJsonException
     */
    private val lenientGson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        Log.d("EVAC_DEBUG", "HTTP Request --> URL: ${request.url}")
        val startTime = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            Log.d("EVAC_DEBUG", "HTTP Response <-- URL: ${request.url} | Status: ${response.code} (${durationMs}ms)")
            response
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "HTTP Exception <-- URL: ${request.url} | Error: ${e.message}", e)
            throw e
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Accept", "application/json")

            // Adjunta el Token automáticamente si existe y no viene en la petición
            authToken?.let { token ->
                if (originalRequest.header("Authorization") == null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(lenientGson)) // 👈 Gson Permisivo Activado
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val userApiService: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    val incidentApiService: IncidentApiService by lazy {
        retrofit.create(IncidentApiService::class.java)
    }

    val safeZonesApi: SafeZonesApiService by lazy {
        retrofit.create(SafeZonesApiService::class.java)
    }

    val pointsOfInterestApi: PointsOfInterestApi by lazy {
        retrofit.create(PointsOfInterestApi::class.java)
    }

    // Módulo de Emergencias Oficiales (Polígonos de zona roja)
    val emergenciesApiService: EmergenciesApiService by lazy {
        retrofit.create(EmergenciesApiService::class.java)
    }

    val mapGraphApiService: MapGraphApiService by lazy {
        retrofit.create(MapGraphApiService::class.java)
    }
}