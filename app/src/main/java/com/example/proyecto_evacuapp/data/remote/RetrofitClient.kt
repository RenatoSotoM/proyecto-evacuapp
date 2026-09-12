package com.example.proyecto_evacuapp.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.proyecto_evacuapp.data.remote.UserApiService

object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.12:3000/api/v1/"

    private var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            appContext?.let { ctx ->
                val prefs = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("jwt_token", null)
                if (!token.isNullOrEmpty()) requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val apiService: IncidentApiService by lazy { retrofit.create(IncidentApiService::class.java) }
    val userApiService: UserApiService by lazy { retrofit.create(UserApiService::class.java) }
}
    // Una sola instancia Retrofit compartida
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: IncidentApiService by lazy { retrofit.create(IncidentApiService::class.java) }
    val authApiService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
}