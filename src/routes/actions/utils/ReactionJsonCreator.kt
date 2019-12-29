package routes.actions.utils

import utils.toJson

object ReactionJsonCreator {

    fun create(channelId: String, messageTs: String) = """
        {
            "channel": "$channelId",
            "name": "sync",
            "timestamp": "$messageTs",
        }
    """.toJson()
}