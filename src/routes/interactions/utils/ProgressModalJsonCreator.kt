package routes.interactions.utils

import enums.CallbackId
import utils.SlackJsonCreator.createPlainText
import utils.toJson

object ProgressModalJsonCreator {

    fun create(triggerId: String) = """
        {
            "trigger_id": "$triggerId",
            "view": ${createView()}
        }
    """.toJson()

}
private fun createView() = """
        {
            "type": "modal",
            "callback_id": "${CallbackId.CLOSE_PROGRESS_MODAL.name.toLowerCase()}",
            "title": ${createPlainText("등록 요청을 보냈습니다.")},
            "close": ${createPlainText("확인")},
            "clear_on_close": true,
            "blocks": []
        }
    """
