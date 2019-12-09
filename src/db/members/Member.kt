package db.members

import io.jsondb.annotation.Document
import io.jsondb.annotation.Id

@Document(collection = "members", schemaVersion = "2.0")
data class Member(
    @Id var id: String? = null,
    var profile: Profile? = null
) {
    data class Profile(
        var displayName: String? = null
    )
}