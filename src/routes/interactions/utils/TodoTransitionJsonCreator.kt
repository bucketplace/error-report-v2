package routes.interactions.utils

import utils.toJson

object TodoTransitionJsonCreator {

    fun create() = """
        {
            "transition": "601"
        }
    """.toJson()
}