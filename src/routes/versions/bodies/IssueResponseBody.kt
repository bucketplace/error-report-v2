package routes.versions.bodies

import com.google.gson.annotations.SerializedName

data class IssueResponseBody(
    val fields: Fields
) {
    data class Fields(
        val components: List<Component>,
        @SerializedName("fixVersions")
        val fixVersions: List<FixVersion>
    ) {
        data class Component(
            val name: String
        )

        data class FixVersion(
            val name: String
        )
    }
}