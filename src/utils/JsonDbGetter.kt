package utils

import io.jsondb.JsonDBTemplate

// http://jsondb.io/
object JsonDbGetter {

    inline fun <reified T> get(dbFilesLocation: String, baseScanPackage: String): JsonDBTemplate {
        return JsonDBTemplate(dbFilesLocation, baseScanPackage).apply {
            if (!collectionExists(T::class.java)) {
                createCollection(T::class.java)
            }
        }
    }
}