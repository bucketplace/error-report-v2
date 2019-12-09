package utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import secrets.GithubSecrets

object GithubApiRequester {

    fun get(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3.raw")
            .header("Authorization", "token ${GithubSecrets.ACCESS_TOKEN}")
            .build()
        return OkHttpClient().newCall(request).execute()
    }
}