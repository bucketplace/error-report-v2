package routes.actions

import io.ktor.application.ApplicationCall
import io.ktor.response.respondText
import routes.actions.requests.ActionRequestBody
import utils.RequestProcessor

class EventSubscriptionVerificationProcessor(
    call: ApplicationCall,
    private val requestBody: ActionRequestBody
) : RequestProcessor(call) {

    override suspend fun process() {
        respondChallenge()
    }

    private suspend fun respondChallenge() {
        call.respondText { requestBody.challenge }
    }
}