package utils

import okhttp3.Headers
import secrets.GithubSecrets

object GithubApiRequester {

    suspend inline fun <reified T> get(url: String): T {
        return ApiRequester.request("GET", url, createHeaders())
    }

    fun createHeaders(): Headers {
        return Headers.of(
            mapOf(
                "Accept" to "application/vnd.github.v3.raw",
                "Authorization" to "token ${GithubSecrets.ACCESS_TOKEN}"
            )
        )
    }
}