package org.cycb.canvas.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ApiConfig {
    const val BASE_URL = "https://cycb-backend.onrender.com/api/"
    const val SOCKET_URL = "https://cycb-backend.onrender.com"

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Проверяем доступность сервера (пингуем его)
        val url = URL(BASE_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000 // 5 секунд таймаут
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode !in 200..399) {
            throw Exception("Server returned code $responseCode")
        }
    }
}