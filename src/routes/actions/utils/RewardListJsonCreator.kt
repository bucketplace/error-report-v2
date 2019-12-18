package routes.actions.utils

import utils.SlackJsonCreator.createDivider
import utils.SlackJsonCreator.createMarkdownText
import utils.toJson

object RewardListJsonCreator {

    fun create(userId: String) = """
        {
            "user_id": "$userId",
            "view": ${createView()} 
        }
    """.toJson()

    private fun createView() = """
        {
            "type": "home",
            "blocks": [
                ${createInformationSection()},
                ${createDivider()}
            ]
        }
    """

    private fun createInformationSection() = """
        {
			"type": "section",
			"text": ${createMarkdownText("※ *스크린샷* 은 스레드에 댓글로 달아주세요.")}
		}
    """
}