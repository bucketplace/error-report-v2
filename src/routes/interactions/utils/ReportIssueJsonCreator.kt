package routes.interactions.utils

import db.members.MemberDao
import db.versions.Version
import db.versions.VersionDao
import enums.*
import enums.Channel.WEB_RELEASE
import kotlinx.coroutines.runBlocking
import routes.interactions.bodies.ViewSubmissionRequestBody
import routes.versions.utils.ServerVersionsGetter
import routes.versions.utils.VersionsGetter
import utils.convertUtf8mb4
import utils.escapeNewLine
import utils.toJson
import kotlin.math.min

class ReportIssueJsonCreator(
    viewSubmissionRequestBody: ViewSubmissionRequestBody
) {

    companion object {
        private const val PROJECT_FIELD_OK_KANBAN_ID = "10400"
        private const val ISSUE_FIELD_BUG_TYPE_ID = "10103"
        private const val REPORTER_FIELD_VALUE = "slack_bug"
    }

    private val path: String
    private val situation: String
    private val expectedResult: String?
    private val version: Version
    private val server: Server?
    private val etcEnvironment: String?
    private val priority: Priority
    private val errorType: ErrorType?
    private val reproducing: Reproducing?
    private val track: Track
    private val developer: Developer
    private val channel: Channel
    private val reporterNickname: String

    init {
        val submissionValues = viewSubmissionRequestBody.view.state.values
        path = submissionValues.path.action.value!!
        situation = submissionValues.situation.action.value!!
        expectedResult = submissionValues.expectedResult.action.value
        version = runBlocking {
            getVersion(submissionValues.version.action.selectedOption!!.value)
        }
        server = submissionValues.server.action.selectedOption?.let { Server.get(it.value) }
        etcEnvironment = submissionValues.etcEnvironment.action.value
        priority = Priority.get(submissionValues.priority.action.selectedOption!!.value)
        errorType = submissionValues.errorType.action.selectedOption?.let { ErrorType.get(it.value) }
        reproducing = submissionValues.reproducing.action.selectedOption?.let { Reproducing.get(it.value) }
        track = Track.get(submissionValues.track.action.selectedOption!!.value)
        developer = Developer.get(submissionValues.developer.action.selectedOption!!.value)
        channel = Channel.get(submissionValues.channel.action.selectedOption!!.value)
        reporterNickname = MemberDao().getMember(viewSubmissionRequestBody.user.id).profile!!.displayName!!
    }

    private suspend fun getVersion(displayName: String): Version {
        if (displayName == ServerVersionsGetter.DISPLAY_NAME) {
            val serverVersions = getServerVersions()
            return if (channel == WEB_RELEASE) {
                serverVersions.filter { it.released }.maxBy { getServerVersionNameValue(it) }
            } else {
                serverVersions.filter { !it.released }.minBy { getServerVersionNameValue(it) }
            } ?: throw Exception("version not found.")
        }
        return (VersionDao().getVersions().find { it.name == displayName }
            ?: throw Exception("version not found."))
    }

    private suspend fun getServerVersions(): List<Version> {
        return VersionsGetter.getAllVersions().filter {
            it.name?.contains("Server ") ?: false
        }
    }

    private fun getServerVersionNameValue(version: Version): Int {
        return version.name
            ?.let { Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(it)?.groupValues }
            ?.let { groupValues -> groupValues[1].toInt() * 10000 + groupValues[2].toInt() * 100 + groupValues[3].toInt() }
            ?: Int.MAX_VALUE
    }

    fun createJson() = """
        {
            "fields": {
                "project": { 
                    "id": "$PROJECT_FIELD_OK_KANBAN_ID"
                },
                 "issuetype": { 
                    "id": "$ISSUE_FIELD_BUG_TYPE_ID"
                },
                "summary": "${createSummary()}",
                "assignee": {
                    "name": "${developer.jiraUserName}"
                },
                "reporter": {
                    "name": "$REPORTER_FIELD_VALUE"
                }, 
                "priority": {
                    "id": "${priority.jiraPriorityFieldValueId}"
                },
                "versions": [
                    {
                        "id": "${version.id}"
                    }
                ],
                "description": "${createDescription()}",
                "components": [
                    {
                        "id": "${developer.platform.jiraComponentFieldValueId}"
                    }
                ],
                "customfield_11100": {
                    "id": "${errorType?.jiraErrorTypeFieldValueId ?: ErrorType.JIRA_ERROR_TYPE_FIELD_VALUE_ID_NONE}" 
                },
                "customfield_11101": {
                    "id": "${channel.jiraReferrerFieldValueId}"
                },
                "customfield_11102": {
                    "id": "${reproducing?.jiraReproducingFieldValueId ?: Reproducing.JIRA_REPRODUCING_FIELD_VALUE_ID_NONE}"
                },
                "labels": [
                    "${track.jiraLabelFieldValue}"
                ]
            }
        }
    """.toJson()

    private fun createSummary(): String {
        return "[${developer.platform.displayName}]"
            .plus(" ${situation.convertUtf8mb4()}")
            .let { it.substring(0, min(it.length, 100)) }
    }

    fun createDescription(): String {
        return StringBuffer().apply {
            append("\nh2. 보고자\n\n${reporterNickname}")
            append("\nh2. 발생 경로\n\n${path}")
            append("\nh2. 오류 현상\n\n${situation}")
            append("\nh2. 기대 결과\n\n${expectedResult ?: "-"}")
            append("\nh2. 발생 버전\n\n${version.name}")
            append("\nh2. 서버\n\n${server?.displayName ?: "-"}")
            append("\nh2. 기타 환경\n\n${etcEnvironment ?: "-"}")
            append("\nh2. 심각도\n\n${priority.displayName}")
            append("\nh2. 예상 오류 유형\n\n${errorType?.displayName ?: "-"}")
            append("\nh2. 재현 가능 여부\n\n${reproducing?.displayName ?: "-"}")
            append("\nh2. 예상 담당트랙\n\n${track.displayName}")
            append("\nh2. 예상 담당개발자\n\n${developer.displayName}")
            append("\nh2. 리포팅 채널\n\n${channel.displayName}")
        }.toString()
            .convertUtf8mb4()
            .escapeNewLine()
    }
}