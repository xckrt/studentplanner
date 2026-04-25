package com.xckrt.studentplanner

import android.util.Log
import com.xckrt.studentplanner.data.AuthManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = AuthManager.token
        Log.d("AuthInterceptor", "Курьер взял токен: $token")
        if (token.isNullOrBlank()) {
            Log.e("AuthInterceptor", "ВНИМАНИЕ: Токен пустой! Отправляю запрос без бейджика.")
            return chain.proceed(originalRequest)
        }
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
    }
}