package routes.home.responses

data class SearchingResponseBody(
    val messages: Messages
) {
    data class Messages(
        val total: Int,
        val pagination: Pagination,
        val paging: Paging,
        val matches: List<Match>
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

        data class Match(
            val blocks: List<Block>?
        ) {
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