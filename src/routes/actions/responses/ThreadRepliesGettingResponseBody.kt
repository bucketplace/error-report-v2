package routes.actions.responses

data class ThreadRepliesGettingResponseBody(
    val messages: List<Message>
) {
    data class Message(
        val botId: String,
        val blocks: List<Block>
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