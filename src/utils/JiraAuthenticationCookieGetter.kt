package utils

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import org.koin.core.KoinComponent
import secrets.JiraSecrets

object JiraAuthenticationCookieGetter {

    private const val LOGIN_URL = "${JiraSecrets.DOMAIN}/rest/auth/1/session"

    suspend fun get(): String {
        return getCookie(getLoginResponseHeaders())
    }

    private suspend fun getLoginResponseHeaders(): Headers {
        return ApiRequester.request<Response>("POST", LOGIN_URL, jsonBody = JiraSecrets.LOGIN_POST_BODY).headers()
    }

    private fun getCookie(headers: Headers): String {
        return headers.toMultimap().entries
            .first { it.key.toLowerCase() == "set-cookie" }.value
            .joinToString("; ")
    }
}