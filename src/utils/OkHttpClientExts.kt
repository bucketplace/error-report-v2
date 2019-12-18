package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Request.Builder

suspend fun OkHttpClient.request(
    method: String,
    url: String,
    headers: Headers? = null,
    jsonBody: String? = null
): Response {
    return withContext(Dispatchers.IO) {
        val request = Builder()
            .url(url)
            .also { builder -> headers?.let { builder.headers(headers) } }
            .method(
                method,
                jsonBody?.let { RequestBody.create(MediaType.get("application/json"), it) }
            )
            .build()
        return@withContext newCall(request).execute()
    }
}