package db.round_robin_server_developer

import enums.Developer
import io.jsondb.JsonDBTemplate
import utils.JsonDbGetter

class RoundRobinServerDeveloperDao {

    companion object {
        private const val DB_FILES_LOCATION = "./db"
        private const val BASE_SCAN_PACKAGE = "db.round_robin_server_developer"
    }

    private val jsonDb = getJsonDb()

    private fun getJsonDb(): JsonDBTemplate {
        return JsonDbGetter.get<RoundRobinServerDeveloper>(DB_FILES_LOCATION, BASE_SCAN_PACKAGE)
    }

    fun clearAndInsertDeveloper(jiraUserName: String) {
        jsonDb.dropCollection(RoundRobinServerDeveloper::class.java)
        jsonDb.createCollection(RoundRobinServerDeveloper::class.java)
        jsonDb.insert(listOf(RoundRobinServerDeveloper(jiraUserName)), RoundRobinServerDeveloper::class.java)
    }

    fun getDeveloper(): Developer {
        val jiraUserName = jsonDb.getCollection(RoundRobinServerDeveloper::class.java).getOrNull(0)?.jiraUserName
        return Developer.values().find { it.jiraUserName == jiraUserName } ?: Developer.JN
    }
}