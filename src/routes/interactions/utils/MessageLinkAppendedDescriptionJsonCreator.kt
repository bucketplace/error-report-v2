package routes.interactions.utils

import secrets.SlackSecrets
import utils.convertUtf8mb4
import utils.escapeNewLine
import utils.toJson

class MessageLinkAppendedDescriptionJsonCreator(
    private val channelId: String,
    private val messageTs: String,
    private val description: String
) {

    fun create() = """
        {
            "fields": {
                "description": "$description${createMessageLinkField()}"
            }
        }
    """.toJson()

    private fun createMessageLinkField(): String {
        @Suppress("ComplexRedundantLet")
        return "\nh2. 슬랙 링크\n\n${getMessageLink(channelId, messageTs)}"
            .convertUtf8mb4()
            .escapeNewLine()
    }

    private fun getMessageLink(channelId: String, messageTs: String): String {
        return "${SlackSecrets.WORKSPACE_DOMAIN}/archives/$channelId/p${messageTs.replace(".", "")}"
    }
}