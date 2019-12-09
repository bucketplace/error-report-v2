package routes.actions.utils

import utils.toJson

object ReactionAddingJsonCreator {

    fun create(channelId: String, messageTs: String) = """
        {
            "channel": "$channelId",
            "name": "sync",
            "timestamp": "$messageTs",
        }
    """.toJson()
}