package routes.interactions.responses

data class ViewOpenResponseBody(
    val ok: Boolean,
    val view: View
) {
    data class View(
        val id: String
    )
}