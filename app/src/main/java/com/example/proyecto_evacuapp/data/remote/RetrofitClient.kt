package com.example.proyecto_evacuapp.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 📱 Para dispositivo físico conectado a tu Wi-Fi:
    private const val BASE_URL = "http://192.168.100.12:3000/api/v1/"

    // 💻 Para emulador de Android Studio:
    // private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"

    // Almacenamiento temporal en memoria del Token JWT (reemplazar por EncryptedSharedPreferences/DataStore en prod)
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

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val userApiService: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    val incidentApiService: IncidentApiService by lazy {
        retrofit.create(IncidentApiService::class.java)
    }
}