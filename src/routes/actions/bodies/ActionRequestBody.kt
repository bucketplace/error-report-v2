package routes.actions.bodies

data class ActionRequestBody(
    val type: String,
    val challenge: String,
    val event: Event
) {
    data class Event(
        val type: String,
        val subtype: String?,
        val text: String?,
        val files: List<File>?,
        val ts: String,
        val threadTs: String?,
        val channel: String,
        val user: String?
    ) {
        data class File(
            val mimetype: String,
            val urlPrivateDownload: String
        )
    }
}