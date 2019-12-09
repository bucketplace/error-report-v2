package routes.interactions

import enums.CallbackId.CREATE_REPORT
import enums.Channel
import io.ktor.application.ApplicationCall
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receive
import io.ktor.response.respond
import kotlinx.coroutines.runBlocking
import routes.interactions.bodies.IssueCreatingResponseBody
import routes.interactions.bodies.MessagePostingResponseBody
import routes.interactions.bodies.ViewOpenResponseBody
import routes.interactions.bodies.ViewSubmissionRequestBody
import routes.interactions.utils.*
import secrets.JiraSecrets
import secrets.SlackSecrets
import utils.JiraApiRequester
import utils.RequestProcessor
import utils.SlackApiRequester
import utils.parseJson

class InteractionsProcessor(call: ApplicationCall) : RequestProcessor(call) {

    companion object {
        private const val API_DOMAIN = "https://slack.com/api"
        private const val VIEW_OPEN_URL = "$API_DOMAIN/views.open"
        private const val VIEW_UPDATE_URL = "$API_DOMAIN/views.update"
        private const val MESSAGE_POSTING_URL = "$API_DOMAIN/chat.postMessage"
        private const val ISSUE_CREATING_URL = "${JiraSecrets.DOMAIN}/rest/api/2/issue"
    }

    private val viewSubmissionRequestBody: ViewSubmissionRequestBody = runBlocking {
        try {
            call.receive<Parameters>()["payload"]!!
                .also { println(it) }
                .parseJson<ViewSubmissionRequestBody>()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun process() {
        if (isReportCreatingRequest()) {
            respondAccepted()
            progressWithProgressModal {
                val issueKey = createReportIssueAndDoTodoTranslation()
                val messageTs = postMessageReportCreated(issueKey)
                // TODO 우선순위 HIGHEST는 SavedMessage의 슬랙링크를 PO채널에 보내야 함
                appendMessageLinkToIssue(issueKey, messageTs)
            }
        }
    }

    private fun isReportCreatingRequest(): Boolean {
        return viewSubmissionRequestBody.view.callbackId == CREATE_REPORT.name.toLowerCase()
    }

    private suspend fun respondAccepted() {
        call.respond(status = HttpStatusCode.Accepted, message = "")
    }

    private suspend fun progressWithProgressModal(progress: suspend () -> Unit) {
        val modalViewId = openProgressModal()
        progress.invoke()
        updateWithCompleteModal(modalViewId)
    }

    private suspend fun openProgressModal(): String {
        val json = ProgressModalJsonCreator.create(viewSubmissionRequestBody.triggerId)
        return SlackApiRequester.post<String>(VIEW_OPEN_URL, json)
            .parseJson<ViewOpenResponseBody>()
            .view.id
    }

    private suspend fun createReportIssueAndDoTodoTranslation(): String {
        return createReportIssue()
            .also { issueKey -> doTodoTransition(issueKey) }
    }

    private suspend fun createReportIssue(): String {
        val json = ReportIssueJsonCreator(viewSubmissionRequestBody).createJson()
        return JiraApiRequester.post<String>(ISSUE_CREATING_URL, json)
            .parseJson<IssueCreatingResponseBody>()
            .key
    }

    private suspend fun doTodoTransition(issueKey: String) {
        val json = TodoTransitionJsonCreator.create()
        JiraApiRequester.post<HttpResponse>(getTransitionUrl(issueKey), json)
    }

    private fun getTransitionUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey/transitions"
    }

    private suspend fun postMessageReportCreated(issueKey: String): String {
        val json = ReportCreatedMessageJsonCreator(viewSubmissionRequestBody, getSlackChannelId(), issueKey).create()
        return SlackApiRequester.post<String>(MESSAGE_POSTING_URL, json)
            .parseJson<MessagePostingResponseBody>()
            .ts
    }

    private fun getSlackChannelId(): String {
        viewSubmissionRequestBody.view.state.values.run {
            if (situation.action.value == "tttt") return SlackSecrets.BSSCCO_TEST_2_CHANEL_ID
            return Channel.get(channel.action.selectedOption!!.value).slackChannelId
        }
    }

    private suspend fun appendMessageLinkToIssue(issueKey: String, messageTs: String) {
        val description = ReportIssueJsonCreator(viewSubmissionRequestBody).createDescription()
        val json = MessageLinkAppendingJsonCreator(getSlackChannelId(), messageTs, description).create()
        JiraApiRequester.put<HttpResponse>(getIssueApiUrl(issueKey), json)
    }

    private fun getIssueApiUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey"
    }

    private suspend fun updateWithCompleteModal(viewId: String) {
        val json = CompleteModalJsonCreator.create(viewId)
        SlackApiRequester.post<HttpResponse>(VIEW_UPDATE_URL, json)
    }
}