package com.example.piggybank.network

import android.content.Context
import com.example.piggybank.data.DeviceIdProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://piggybank-api-eight.vercel.app/api/"

    @Volatile
    private var deviceIdProvider: DeviceIdProvider? = null

    fun initialize(context: Context) {
        if (deviceIdProvider == null) {
            synchronized(this) {
                if (deviceIdProvider == null) {
                    deviceIdProvider = DeviceIdProvider.getInstance(context.applicationContext)
                }
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val deviceId = deviceIdProvider?.getDeviceId()
                val requestBuilder = chain.request().newBuilder()
                if (!deviceId.isNullOrBlank()) {
                    requestBuilder.header(DeviceIdProvider.HEADER_DEVICE_ID, deviceId)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
