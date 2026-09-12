package com.example.proyecto_evacuapp.data.remote

import android.annotation.SuppressLint
import android.content.Context
import com.example.proyecto_evacuapp.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@SuppressLint("StaticFieldLeak")
object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.12:3000/api/v1/"
    private lateinit var appContext: Context
    private lateinit var tokenManager: TokenManager

    fun init(context: Context) {
        appContext = context.applicationContext
        tokenManager = TokenManager(appContext)
    }

    // Interceptor para inyectar automáticamente el token JWT
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = if (::tokenManager.isInitialized) tokenManager.getToken() else null

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Servicios expuestos directamente para mantener compatibilidad en todo el proyecto
    val userApiService: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val incidentApiService: IncidentApiService by lazy {
        retrofit.create(IncidentApiService::class.java)
    }
}