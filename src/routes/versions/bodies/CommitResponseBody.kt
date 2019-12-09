package routes.versions.bodies

data class CommitResponseBody(
    val commit: Commit
) {
    data class Commit(
        val message: String
    )
}