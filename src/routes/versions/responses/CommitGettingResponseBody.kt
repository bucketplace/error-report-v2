package routes.versions.responses

data class CommitGettingResponseBody(
    val commit: Commit
) {
    data class Commit(
        val message: String
    )
}