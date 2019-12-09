package routes.versions.bodies

data class VersionsResponseBody(
    val data: List<Data>
) {
    data class Data(
        val attributes: Attributes
    ) {
        data class Attributes(
            val version: String
        )
    }
}