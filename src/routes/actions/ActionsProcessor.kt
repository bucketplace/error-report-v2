package routes.actions

import enums.Action
import io.ktor.application.ApplicationCall
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.io.copyAndClose
import kotlinx.coroutines.runBlocking
import routes.actions.bodies.ActionRequestBody
import routes.actions.bodies.ThreadRepliesResponseBody
import routes.actions.bodies.ThreadRepliesResponseBody.Message
import routes.actions.utils.CommentAddingJsonCreator
import routes.actions.utils.ReactionAddingJsonCreator
import routes.actions.utils.RewardListJsonCreator
import secrets.JiraSecrets
import secrets.SlackSecrets
import utils.*
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.ws.http.HTTPException
import kotlin.math.min

class ActionsProcessor(call: ApplicationCall) : RequestProcessor(call) {

    companion object {
        private const val SLACK_API_DOMAIN = "https://slack.com/api"
        private const val THREAD_REPLIES_URL = "$SLACK_API_DOMAIN/channels.replies"
        private const val REACTION_ADDING_URL = "$SLACK_API_DOMAIN/reactions.add"
        private const val VIEW_PUBLISH_URL = "$SLACK_API_DOMAIN/views.publish"
        private const val SEARCH_MESSAGES_URL = "$SLACK_API_DOMAIN/search.messages"
        private val ISSUE_KEY_REGEX = Regex("browse/(OK-\\d+)")
    }

    private val actionRequestBody: ActionRequestBody = runBlocking {
        try {
            call.receive<String>()
                .also { println(it) }
                .parseJson<ActionRequestBody>()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun process() {
        if (isEventSubscriptionVerificationRequest()) {
            respondChallenge()
        } else if (isEventCallback()) {
            respondAccepted()
            if (isAppHomeOpened()) {
                handleAppHomeOpened()
            } else if (isMessagePosted()) {
                handleMessagePosted()
            } else if (isMessageChanged()) {
//                updateComment(actionRequestBody.event.message!!.clientMsgId)
            } else if (isMessageDeleted()) {
//                deleteComment(actionRequestBody.event.previousMessage!!.clientMsgId)
            }
        }
    }

    private fun isEventSubscriptionVerificationRequest(): Boolean {
        return actionRequestBody.type == Action.URL_VERIFICATION.name.toLowerCase()
    }

    private suspend fun respondChallenge() {
        call.respondText { actionRequestBody.challenge }
    }

    private fun isEventCallback(): Boolean {
        return actionRequestBody.type == Action.EVENT_CALLBACK.name.toLowerCase()
    }

    private suspend fun respondAccepted() {
        call.respond(status = HttpStatusCode.Accepted, message = "")
    }

    private fun isAppHomeOpened(): Boolean {
        return actionRequestBody.event.type == "app_home_opened"
    }

    private suspend fun handleAppHomeOpened() {
        publishRewardList(getThisMonthReportMessages())
    }

    private suspend fun getThisMonthReportMessages(): List<String> {
        val messages = ArrayList<String>()
//        do {
//            val response = SlackApiRequester.get<SearchResponseBody>(SEARCH_MESSAGES_URL)
//                .members
//        }while ()
        return messages
    }

    private fun getSearchMessagedUrl(page: Int) {

    }

    private suspend fun publishRewardList(messages: List<String>) {
        SlackApiRequester.post<Unit>(VIEW_PUBLISH_URL, RewardListJsonCreator.create(actionRequestBody.event.user!!))
    }

    private fun isMessagePosted(): Boolean {
        return listOf(null, "file_share").any { it == actionRequestBody.event.subtype }
    }

    private suspend fun handleMessagePosted() {
        findReportIssueKey()?.let { issueKey ->
            postCommentToIssue(issueKey)
            addSyncReactionToMessage()
        }
    }

    private suspend fun findReportIssueKey(): String? {
        return findIssueKey(getThreadFirstMessage())
    }

    private suspend fun getThreadFirstMessage(): Message {
        return SlackApiRequester.get<ThreadRepliesResponseBody>(getThreadRepliesUrl(), SlackSecrets.APP_ACCESS_TOKEN).messages[0]
    }

    private fun getThreadRepliesUrl(): String {
        return actionRequestBody.event.let {
            "${THREAD_REPLIES_URL}?channel=${it.channel}&thread_ts=${it.threadTs}"
        }
    }

    private fun findIssueKey(message: Message): String? {
        if (message.botId == SlackSecrets.BOT_ID) {
            val texts = message.blocks.joinToString { it.text.text }
            return ISSUE_KEY_REGEX.find(texts)?.groupValues?.get(1)
        }
        return null
    }

    private suspend fun postCommentToIssue(issueKey: String) {
        val json = createCommentAddingJson(uploadFiles(issueKey))
        JiraApiRequester.post<Unit>(getCommentAddingUrl(issueKey), json)
    }

    private suspend fun uploadFiles(issueKey: String): ArrayList<String>? {
        return actionRequestBody.event.files?.fold(ArrayList()) { attachments, file ->
            attachments.apply {
                val tempFile = downloadAsTempFile(file.urlPrivateDownload)
                add(uploadFileAsAttachment(tempFile, file.mimetype, issueKey))
                tempFile.delete()
            }
        }
    }

    private suspend fun downloadAsTempFile(url: String): File {
        File("temp").mkdir()
        val file = File("temp/${File(url).name}").also { it.createNewFile() }
        HttpClientCreator.create().use { client ->
            val response = client.get<HttpResponse>(url) {
                header("Authorization", "Bearer ${SlackSecrets.BOT_ACCESS_TOKEN}")
            }
            if (!response.status.isSuccess()) {
                throw HTTPException(response.status.value)
            }
            response.content.copyAndClose(file.writeChannel())
        }
        return file
    }

    private suspend fun uploadFileAsAttachment(file: File, mimeType: String, issueKey: String): String {
        var outputStream: DataOutputStream? = null

        val lineEnd = "\r\n"
        val twoHyphens = "--"
        val boundary = "*****"

        var bytesRead: Int
        var bytesAvailable: Int
        var bufferSize: Int
        val buffer: ByteArray
        val maxBufferSize = 1 * 1024
        try {
            val url = URL(getAttachmentAddingUrl(issueKey))
            val connection = url.openConnection() as HttpURLConnection

            connection.doInput = true
            connection.doOutput = true
            connection.useCaches = false
            connection.setChunkedStreamingMode(1024)
            connection.requestMethod = "POST"
            connection.setRequestProperty("cookie", JiraAuthenticationCookieGetter.get())
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("X-Atlassian-Token", "no-check")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)

//        val token = "anyvalye"
//        outputStream!!.writeBytes("Content-Disposition: form-data; name=\"Token\"$lineEnd")
//        outputStream!!.writeBytes("Content-Type: text/plain;charset=UTF-8$lineEnd")
//        outputStream!!.writeBytes("Content-Length: " + token.length + lineEnd)
//        outputStream!!.writeBytes(lineEnd)
//        outputStream!!.writeBytes(token + lineEnd)
//        outputStream!!.writeBytes(twoHyphens + boundary + lineEnd)
//
//        val taskId = "anyvalue"
//        outputStream!!.writeBytes("Content-Disposition: form-data; name=\"TaskID\"$lineEnd")
//        outputStream!!.writeBytes("Content-Type: text/plain;charset=UTF-8$lineEnd")
//        outputStream!!.writeBytes("Content-Length: " + taskId.length + lineEnd)
//        outputStream!!.writeBytes(lineEnd)
//        outputStream!!.writeBytes(taskId + lineEnd)
//        outputStream!!.writeBytes(twoHyphens + boundary + lineEnd)

            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"${file.name}\"$lineEnd")
            outputStream.writeBytes("Content-Type: $mimeType$lineEnd")
            outputStream.writeBytes(lineEnd)

            val fileInputStream = FileInputStream(file)
            bytesAvailable = fileInputStream.available()
            bufferSize = min(bytesAvailable, maxBufferSize)
            buffer = ByteArray(bufferSize)

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            try {
                while (bytesRead > 0) {
                    try {
                        outputStream.write(buffer, 0, bufferSize)
                    } catch (e: OutOfMemoryError) {
                        e.printStackTrace()
                        throw e
                    }

                    bytesAvailable = fileInputStream.available()
                    bufferSize = min(bytesAvailable, maxBufferSize)
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }

            outputStream.writeBytes(lineEnd)
            outputStream.writeBytes(
                twoHyphens + boundary + twoHyphens
                        + lineEnd
            )

            // Responses from the server (code and message)
            val serverResponseCode = connection.getResponseCode()
            val serverResponseMessage = connection.getResponseMessage()
            println("Server Response Code  $serverResponseCode")
            println("Server Response Message $serverResponseMessage")

            fileInputStream.close()
            outputStream.flush()

            connection.inputStream
            //for android InputStream is = connection.getInputStream();
            val inputStream = connection.inputStream

            var ch: Int
            val b = StringBuffer()
            ch = inputStream.read()
            while (ch != -1) {
                b.append(ch.toChar())
                ch = inputStream.read()
            }

            val responseString = b.toString()
            println("response string is$responseString") //Here is the actual output

            outputStream.close()
        } catch (ex: Exception) {
            println("Send file Exception" + ex.message + "")
            ex.printStackTrace()
            throw ex
        }

        return file.name
    }

    private fun getAttachmentAddingUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey/attachments"
    }

    private fun createCommentAddingJson(attachments: List<String>?): String {
        return CommentAddingJsonCreator(actionRequestBody.event.text!!, attachments).create()
    }

    private fun getCommentAddingUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey/comment"
    }

    private suspend fun addSyncReactionToMessage() {
        SlackApiRequester.post<Unit>(REACTION_ADDING_URL, createReactionAddingJson())
    }

    private fun createReactionAddingJson(): String {
        return actionRequestBody.event.let {
            ReactionAddingJsonCreator.create(it.channel, it.ts)
        }
    }

    private fun isMessageChanged(): Boolean {
        return actionRequestBody.event.subtype == "message_changed"
    }

    private fun isMessageDeleted(): Boolean {
        return actionRequestBody.event.subtype == "message_deleted"
    }
}