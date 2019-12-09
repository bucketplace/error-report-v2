package utils

import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import secrets.SlackSecrets

object SlackApiRequester {

    suspend inline fun <reified T> get(
        url: String,
        accessToken: String = SlackSecrets.BOT_ACCESS_TOKEN
    ): T {
        return request(HttpMethod.Get, url, accessToken = accessToken)
    }

    suspend inline fun <reified T> request(
        httpMethod: HttpMethod,
        url: String,
        jsonBody: String = "",
        accessToken: String = SlackSecrets.BOT_ACCESS_TOKEN
    ): T {
        return HttpClientCreator.create().use { client ->
            client.request(url) {
                method = httpMethod
                header("Authorization", "Bearer $accessToken")
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
}