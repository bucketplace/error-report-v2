package utils

import org.json.JSONObject

fun String.escapeNewLine(): String {
    return replace("\n", "\\n")
}

fun String.convertUtf8mb4(): String {
    return replace("[\uD800-\uDBFF][\uDC00-\uDFFF]".toRegex(), "??")
}

inline fun <reified T> String.parseJson(): T {
    return GsonCreator.create().fromJson(this, T::class.java)
}

fun String.toJson(): String {
    return JSONObject(this).toString()
}