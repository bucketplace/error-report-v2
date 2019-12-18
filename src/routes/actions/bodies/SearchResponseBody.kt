package routes.actions.bodies

data class SearchResponseBody(
    val ok: Boolean,
    val query: String,
    val messages: Messages
) {
    data class Messages(
        val total: Int,
        val pagination: Pagination,
        val paging: Paging,
        val matches: List<Matche>
    ) {
        data class Pagination(
            val total_count: Int,
            val page: Int,
            val per_page: Int,
            val page_count: Int,
            val first: Int,
            val last: Int
        )

        data class Paging(
            val count: Int,
            val total: Int,
            val page: Int,
            val pages: Int
        )

        data class Matche(
            val channel: Channel,
            val username: String,
            val ts: String,
            val blocks: List<Block>
        ) {
            data class Channel(
                val id: String,
                val name: String
            )

            data class Block(
                val text: Text
            ) {
                data class Text(
                    val text: String
                )
            }
        }
    }
}