package utils

import io.ktor.client.call.typeInfo
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import org.koin.core.KoinComponent

object ApiRequester : KoinComponent {

    suspend inline fun <reified T> request(
        method: String,
        url: String,
        headers: Headers? = null,
        jsonBody: String? = null
    ): T {
        return getKoin()
            .get<OkHttpClient>()
            .request(method, url, headers, jsonBody)
            .use {
                receive(it)
            }
    }

    inline fun <reified T> receive(response: Response): T {
        return when (typeInfo<T>()) {
            typeInfo<Unit>() -> Unit as T
            typeInfo<Response>() -> response as T
            typeInfo<String>() -> response.body()!!.string() as T
            else -> response.body()!!.string().parseJson()
        }
    }
}