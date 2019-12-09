package db.versions

import io.jsondb.annotation.Document
import io.jsondb.annotation.Id

@Document(collection = "versions", schemaVersion = "2.0")
data class Version(
    @Id var id: String? = null,
    @Id var name: String? = null,
    var released: Boolean = false
)
