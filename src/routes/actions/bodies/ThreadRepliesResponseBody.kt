package routes.actions.bodies

data class ThreadRepliesResponseBody(
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