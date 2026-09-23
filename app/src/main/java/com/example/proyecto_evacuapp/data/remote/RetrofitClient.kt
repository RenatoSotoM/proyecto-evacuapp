package com.example.proyecto_evacuapp.data.remote

import android.os.Build
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/v1/"
    private const val PHYSICAL_BASE_URL = "http://192.168.180.55:3000/api/v1/"

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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

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
            .addConverterFactory(GsonConverterFactory.create())
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
}