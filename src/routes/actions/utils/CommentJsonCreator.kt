package routes.actions.utils

import db.members.MemberDao
import utils.escapeNewLine
import utils.toJson

class CommentJsonCreator(
    private val text: String,
    private val attachments: List<String>?
) {

    companion object {
        private val USER_MENTION_REGEX = Regex("<@(.+?)>")
    }

    fun create() = """
        {
            "body": "${createBody()}"
        }
    """.toJson()

    private fun createBody(): String {
        return StringBuffer(text)
            .apply { replaceMemberIdToNickname() }
            .apply { appendAttachments() }
            .toString()
            .escapeNewLine()
    }

    private fun StringBuffer.replaceMemberIdToNickname() {
        USER_MENTION_REGEX.findAll(text).forEach {
            replace(
                it.range.first,
                it.range.last + 1,
                "@${getDisplayName(it.groupValues[1])}"
            )
        }
    }

    private fun StringBuffer.appendAttachments() {
        attachments?.forEach { append("\n!$it|width=600!") }
    }

    private fun getDisplayName(id: String): String {
        return MemberDao().getMember(id).profile?.displayName ?: "????"
    }
}