package routes.actions

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import okhttp3.Response
import okio.Okio
import routes.actions.requests.ActionRequestBody
import routes.actions.responses.ThreadRepliesGettingResponseBody
import routes.actions.responses.ThreadRepliesGettingResponseBody.Message
import routes.actions.utils.CommentJsonCreator
import routes.actions.utils.ReactionJsonCreator
import secrets.JiraSecrets
import secrets.SlackSecrets
import utils.JiraApiRequester
import utils.JiraAuthenticationCookieGetter
import utils.RequestProcessor
import utils.SlackApiRequester
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

class MessagePostedProcessor(
    call: ApplicationCall,
    private val requestBody: ActionRequestBody
) : RequestProcessor(call) {

    companion object {
        private const val SLACK_API_DOMAIN = "https://slack.com/api"
        private const val THREAD_REPLIES_GETTING_URL = "$SLACK_API_DOMAIN/channels.replies"
        private const val REACTION_ADDING_URL = "$SLACK_API_DOMAIN/reactions.add"
        private val ISSUE_KEY_REGEX = Regex("browse/(OK-\\d+)")
    }

    override suspend fun process() {
        respondAccepted()
        findReportIssueKey()?.let { issueKey ->
            postCommentToIssue(issueKey)
            addSyncReactionToMessage()
        }
    }

    private suspend fun respondAccepted() {
        call.respond(status = HttpStatusCode.Accepted, message = "")
    }

    private suspend fun findReportIssueKey(): String? {
        return findIssueKey(getThreadFirstMessage())
    }

    private suspend fun getThreadFirstMessage(): Message {
        return SlackApiRequester.get<ThreadRepliesGettingResponseBody>(
            url = getThreadRepliesGettingUrl(),
            accessToken = SlackSecrets.APP_ACCESS_TOKEN
        ).messages[0]
    }

    private fun getThreadRepliesGettingUrl(): String {
        return requestBody.event.let {
            "${THREAD_REPLIES_GETTING_URL}?channel=${it.channel}&thread_ts=${it.threadTs}"
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
        val json = createCommentJson(uploadFiles(issueKey))
        JiraApiRequester.post<Unit>(getCommentPostingUrl(issueKey), json)
    }

    private suspend fun uploadFiles(issueKey: String): MutableList<String>? {
        return requestBody.event.files?.fold(mutableListOf()) { attachments, file ->
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
        SlackApiRequester.get<Response>(url).use {
            val source = Okio.buffer(it.body()!!.source())
            val sink = Okio.buffer(Okio.sink(file))
            source.readAll(sink)
            source.close()
            sink.close()
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

    private fun createCommentJson(attachments: List<String>?): String {
        return CommentJsonCreator(requestBody.event.text!!, attachments).create()
    }

    private fun getCommentPostingUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey/comment"
    }

    private suspend fun addSyncReactionToMessage() {
        SlackApiRequester.post<Unit>(REACTION_ADDING_URL, createReactionJson())
    }

    private fun createReactionJson(): String {
        return requestBody.event.let {
            ReactionJsonCreator.create(it.channel, it.ts)
        }
    }
}