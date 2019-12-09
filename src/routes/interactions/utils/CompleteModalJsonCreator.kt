package routes.interactions.utils

import enums.CallbackId
import utils.SlackJsonCreator.createPlainText
import utils.toJson

object CompleteModalJsonCreator {

    fun create(viewId: String) = """
        {
            "view_id": "$viewId",
            "view": ${createView()}
        }
    """.toJson()

    private fun createView() = """
        {
            "type": "modal",
            "callback_id": "${CallbackId.CLOSE_COMPLETE_MODAL.name.toLowerCase()}",
            "title": ${createPlainText("등록 완료")},
            "close": ${createPlainText("확인")},
            "clear_on_close": true,
            "blocks": []
        }
    """
}