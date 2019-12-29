package routes.actions

import enums.Action
import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import routes.actions.requests.ActionRequestBody
import utils.RequestProcessor
import utils.parseJson

class ActionsProcessor(call: ApplicationCall) : RequestProcessor(call), KoinComponent {

    private val requestBody = getRequestBody()

    private fun getRequestBody(): ActionRequestBody {
        return runBlocking {
            try {
                call.receive<String>()
                    .also { println(it) }
                    .parseJson<ActionRequestBody>()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun process() {
        if (isEventSubscriptionVerification()) {
            EventSubscriptionVerificationProcessor(call, requestBody).process()
        } else if (isEventCallback()) {
            if (isAppHomeOpened()) {
                AppHomeOpenedProcessor(call, requestBody).process()
            } else if (isMessagePosted()) {
                MessagePostedProcessor(call, requestBody).process()
            } else if (isMessageChanged()) {
//                updateComment(actionRequestBody.event.message!!.clientMsgId)
            } else if (isMessageDeleted()) {
//                deleteComment(actionRequestBody.event.previousMessage!!.clientMsgId)
            }
        }
    }

    private fun isEventSubscriptionVerification(): Boolean {
        return requestBody.type == Action.URL_VERIFICATION.name.toLowerCase()
    }

    private fun isEventCallback(): Boolean {
        return requestBody.type == Action.EVENT_CALLBACK.name.toLowerCase()
    }

    private fun isAppHomeOpened(): Boolean {
        return requestBody.event.type == "app_home_opened"
    }

    private fun isMessagePosted(): Boolean {
        return listOf(null, "file_share").any { it == requestBody.event.subtype }
    }

    private fun isMessageChanged(): Boolean {
        return requestBody.event.subtype == "message_changed"
    }

    private fun isMessageDeleted(): Boolean {
        return requestBody.event.subtype == "message_deleted"
    }
}