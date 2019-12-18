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
            .receive()
    }

    inline fun <reified T> Response.receive(): T {
        return when (typeInfo<T>()) {
            typeInfo<Unit>() -> close().let { Unit as T }
            typeInfo<Response>() -> this as T // notes: not closed.
            typeInfo<String>() -> body()!!.string() as T
            else -> body()!!.string().parseJson()
        }
    }
}