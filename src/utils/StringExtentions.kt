package utils

import com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

fun String.escapeNewLine(): String {
    return replace("\n", "\\n")
}

fun String.escapeDoubleQuotation(): String {
    return replace("\"", "'")
}

fun String.convertUtf8mb4(): String {
    return replace("[\uD800-\uDBFF][\uDC00-\uDFFF]".toRegex(), "??")
}

inline fun <reified T> String.parseJson(): T {
    return GsonBuilder()
        .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
        .create()
        .fromJson(this, object : TypeToken<T>() {}.type)
}

fun String.toJson(): String {
    return JSONObject(this).toString()
}