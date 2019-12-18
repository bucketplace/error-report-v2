package routes.versions.types

data class Commit(
    val serverDomain: String,
    val hash: String
)