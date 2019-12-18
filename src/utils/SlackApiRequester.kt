package utils

import okhttp3.Headers
import okhttp3.OkHttpClient
import org.koin.core.KoinComponent
import secrets.SlackSecrets

object SlackApiRequester {

    suspend inline fun <reified T> get(url: String, accessToken: String = SlackSecrets.BOT_ACCESS_TOKEN): T {
        return ApiRequester.request("GET", url, createHeaders(accessToken))
    }

    fun createHeaders(accessToken: String): Headers {
        return Headers.of(mapOf("Authorization" to "Bearer $accessToken"))
    }

    suspend inline fun <reified T> post(url: String, jsonBody: String): T {
        return ApiRequester.request("POST", url, createHeaders(SlackSecrets.BOT_ACCESS_TOKEN), jsonBody)
    }
}