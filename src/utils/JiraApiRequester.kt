package utils

import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent

object JiraApiRequester {

    suspend inline fun <reified T> get(url: String): T {
        return request(HttpMethod.Get, url)
    }

    suspend inline fun <reified T> request(httpMethod: HttpMethod, url: String, jsonBody: String = ""): T {
        return HttpClientCreator.create().use { client ->
            client.request(url) {
                method = httpMethod
                header("cookie", JiraAuthenticationCookieGetter.get())
                body = TextContent(
                    contentType = Application.Json,
                    text = jsonBody
                )
            }
        }
    }

    suspend inline fun <reified T> post(url: String, jsonBody: String): T {
        return request(HttpMethod.Post, url, jsonBody)
    }

    suspend inline fun <reified T> put(url: String, jsonBody: String): T {
        return request(HttpMethod.Put, url, jsonBody)
    }
}