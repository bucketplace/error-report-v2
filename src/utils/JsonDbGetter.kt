package utils

import io.jsondb.JsonDBTemplate

// http://jsondb.io/
object JsonDbGetter {

    inline fun <reified T> get(dbFileLocation: String, baseScanPackage: String): JsonDBTemplate {
        return JsonDBTemplate(dbFileLocation, baseScanPackage).apply {
            if (!collectionExists(T::class.java)) {
                createCollection(T::class.java)
            }
        }
    }
}