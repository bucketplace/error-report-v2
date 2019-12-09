package routes.interactions.bodies

data class ViewOpenResponseBody(
    val ok: Boolean,
    val view: View
) {
    data class View(
        val id: String
    )
}