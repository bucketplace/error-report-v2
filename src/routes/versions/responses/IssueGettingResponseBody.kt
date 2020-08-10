package routes.versions.responses

import com.google.gson.annotations.SerializedName

data class IssueGettingResponseBody(
    val fields: Fields?
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