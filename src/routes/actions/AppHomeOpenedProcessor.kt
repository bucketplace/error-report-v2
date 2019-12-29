package routes.actions

import io.ktor.application.ApplicationCall
import routes.actions.requests.ActionRequestBody
import utils.RequestProcessor

class AppHomeOpenedProcessor(
    call: ApplicationCall,
    private val requestBody: ActionRequestBody
) : RequestProcessor(call) {

    override suspend fun process() {
//        respondAccepted()
//        publishThisMonthRewardMembers()
    }
}