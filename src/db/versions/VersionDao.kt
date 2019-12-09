package db.versions

import io.jsondb.JsonDBTemplate
import utils.JsonDbGetter

class VersionDao {

    companion object {
        private const val DB_FILES_LOCATION = "./db"
        private const val BASE_SCAN_PACKAGE = "db.versions"
    }

    private val jsonDb = getJsonDb()

    private fun getJsonDb(): JsonDBTemplate {
        return JsonDbGetter.get<Version>(DB_FILES_LOCATION, BASE_SCAN_PACKAGE)
    }

    fun clearAndInsertVersions(versions: List<Version>) {
        jsonDb.dropCollection(Version::class.java)
        jsonDb.createCollection(Version::class.java)
        jsonDb.insert(versions, Version::class.java)
    }

    fun getVersions(): List<Version> {
        return jsonDb.getCollection(Version::class.java)
    }
}