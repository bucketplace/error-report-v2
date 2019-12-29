package routes.versions.responses

data class VersionsGettingResponseBody(
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