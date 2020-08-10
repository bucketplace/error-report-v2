package db.round_robin_server_developer

import io.jsondb.annotation.Document
import io.jsondb.annotation.Id

@Document(collection = "round_robin_server_developer", schemaVersion = "1.0")
data class RoundRobinServerDeveloper(
    @Id var jiraUserName: String? = null
)
