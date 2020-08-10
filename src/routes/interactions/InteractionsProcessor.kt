package routes.interactions

import db.round_robin_server_developer.RoundRobinServerDeveloperDao
import enums.CallbackId.CREATE_REPORT
import enums.Channel
import enums.Developer
import enums.Developer.SNA
import enums.Platform.SERVER
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import routes.interactions.requests.InteractionRequestBody
import routes.interactions.responses.IssueCreatingResponseBody
import routes.interactions.responses.MessagePostingResponseBody
import routes.interactions.utils.*
import secrets.JiraSecrets
import utils.JiraApiRequester
import utils.RequestProcessor
import utils.SlackApiRequester
import utils.parseJson

class InteractionsProcessor(call: ApplicationCall) : RequestProcessor(call) {

    companion object {
        private const val SLACK_API_DOMAIN = "https://slack.com/api"
        private const val VIEW_OPEN_URL = "$SLACK_API_DOMAIN/views.open"
        private const val VIEW_UPDATING_URL = "$SLACK_API_DOMAIN/views.update"
        private const val MESSAGE_POSTING_URL = "$SLACK_API_DOMAIN/chat.postMessage"
        private const val ISSUE_CREATING_URL = "${JiraSecrets.DOMAIN}/rest/api/2/issue"
        private const val BSSCCO_TEST_2_CHANEL_ID = "CQ15ND811"
    }

    private val requestBody = getRequestBody()

    private fun getRequestBody(): InteractionRequestBody {
        return runBlocking {
            call.receive<Parameters>()["payload"]!!.also { println(it) }.parseJson<InteractionRequestBody>().also { println(it) }
        }
    }

    override suspend fun process() {
        if (isReportCreatingRequest()) {
            respondAccepted()
            progressWithProgressModal {
                val issueKey = if (requestBody.hasReleaseReportingChannel()) {
                    createReportIssue()
                } else {
                    createReportIssueAndDoTodoTranslation()
                }
                val messageTs = postReportCreatedMessage(issueKey)
                // TODO 우선순위 HIGHEST는 SavedMessage의 슬랙링크를 PO채널에 보내야 함
                appendMessageLinkToIssue(issueKey, messageTs)

                val displayName = requestBody.view.state.values.developer.action.selectedOption!!.value
                if (displayName == SNA.displayName) {
                    val nextDeveloper = getNextAvailableServerDeveloper(RoundRobinServerDeveloperDao().getDeveloper())
                    RoundRobinServerDeveloperDao().clearAndInsertDeveloper(nextDeveloper.jiraUserName)
                }
            }
        }
    }

    private fun getNextAvailableServerDeveloper(previousDeveloper: Developer): Developer {
        val serverDevelopers = getServerDevelopers()
        return serverDevelopers[(serverDevelopers.indexOf(previousDeveloper) + 1) % serverDevelopers.size]
    }

    private fun getServerDevelopers(): List<Developer> {
        return Developer.values()
            .filter { it.platform == SERVER }
            .filter { it != SNA }
    }

    private fun isReportCreatingRequest(): Boolean {
        return requestBody.view.callbackId == CREATE_REPORT.name.toLowerCase()
    }

    private suspend fun respondAccepted() {
        call.respond(status = HttpStatusCode.Accepted, message = "")
    }

    private suspend fun progressWithProgressModal(progress: suspend () -> Unit) {
//        val modalViewId = openProgressModal()
        progress.invoke()
//        updateWithCompleteModal(modalViewId)
    }

    private suspend fun openProgressModal(): String {
//        println("bsscco")
        val json = ProgressModalJsonCreator.create(requestBody.triggerId)
//        println("triggerID: ${requestBody.triggerId}")
//        println("json: ${json}")
        return try {
            val response = SlackApiRequester.post<Response>(VIEW_OPEN_URL, json)

            println("body: ${response.body()!!.string()}")
            println("")
            "1"
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun createReportIssueAndDoTodoTranslation(): String {
        return createReportIssue().also { issueKey ->
            doTodoTransition(issueKey)
        }
    }

    private suspend fun createReportIssue(): String {
        val json = ReportIssueJsonCreator(requestBody).createJson()
        return JiraApiRequester.post<IssueCreatingResponseBody>(ISSUE_CREATING_URL, json).key
    }

    private suspend fun doTodoTransition(issueKey: String) {
        val json = TodoTransitionJsonCreator.create()
        JiraApiRequester.post<Unit>(getTransitionUrl(issueKey), json)
    }

    private fun getTransitionUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey/transitions"
    }

    private suspend fun postReportCreatedMessage(issueKey: String): String {
        val json = ReportCreatedMessageJsonCreator(requestBody, getSlackChannelId(), issueKey).create()
        return SlackApiRequester.post<MessagePostingResponseBody>(MESSAGE_POSTING_URL, json).ts
    }

    private fun getSlackChannelId(): String {
        requestBody.view.state.values.run {
            if (situation.action.value == "tttt") return BSSCCO_TEST_2_CHANEL_ID
            return Channel.get(channel.action.selectedOption!!.value).slackChannelId
        }
    }

    private suspend fun appendMessageLinkToIssue(issueKey: String, messageTs: String) {
        val description = ReportIssueJsonCreator(requestBody).createDescription()
        val json = MessageLinkAppendedDescriptionJsonCreator(getSlackChannelId(), messageTs, description).create()
        JiraApiRequester.put<Unit>(getIssueUpdatingUrl(issueKey), json)
    }

    private fun getIssueUpdatingUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey"
    }

    private suspend fun updateWithCompleteModal(viewId: String) {
        SlackApiRequester.post<Unit>(VIEW_UPDATING_URL, CompleteModalJsonCreator.create(viewId))
    }
}