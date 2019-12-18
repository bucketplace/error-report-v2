package utils

import okhttp3.Headers

object JiraApiRequester {

    suspend inline fun <reified T> get(url: String): T {
        return ApiRequester.request("GET", url, createHeaders())
    }

    suspend fun createHeaders(): Headers {
        return Headers.of(mapOf("cookie" to JiraAuthenticationCookieGetter.get()))
    }

    suspend inline fun <reified T> post(url: String, jsonBody: String): T {
        return ApiRequester.request("POST", url, createHeaders(), jsonBody)
    }

    suspend inline fun <reified T> put(url: String, jsonBody: String): T {
        return ApiRequester.request("PUT", url, createHeaders(), jsonBody)
    }
}