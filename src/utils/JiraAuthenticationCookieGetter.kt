package utils

import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType.Application
import io.ktor.http.Headers
import io.ktor.http.content.TextContent
import io.ktor.util.toMap
import secrets.JiraSecrets

object JiraAuthenticationCookieGetter {

    private const val LOGIN_URL = "${JiraSecrets.DOMAIN}/rest/auth/1/session"

    suspend fun get(): String {
        val response = loginJira()
        return getCookie(response.headers)
    }

    private suspend fun loginJira(): HttpResponse {
        return HttpClientCreator.create().use { client ->
            client.post(LOGIN_URL) {
                body = TextContent(
                    contentType = Application.Json,
                    text = JiraSecrets.LOGIN_POST_BODY
                )
            }
        }
    }

    private fun getCookie(headers: Headers): String {
        return headers.toMap().entries
            .first { it.key.toLowerCase() == "set-cookie" }.value
            .joinToString("; ")
    }
}