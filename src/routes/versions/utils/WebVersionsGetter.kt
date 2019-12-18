package routes.versions.utils

import db.versions.Version
import enums.Server
import okhttp3.OkHttpClient
import okhttp3.Request
import routes.versions.bodies.CommitResponseBody
import routes.versions.bodies.IssueResponseBody
import routes.versions.types.Commit
import secrets.AwsSecrets
import secrets.GithubSecrets
import secrets.JiraSecrets
import utils.GithubApiRequester
import utils.JiraApiRequester

@Suppress("ComplexRedundantLet", "SimpleRedundantLet")
class WebVersionsGetter(versios: List<Version>) {

    companion object {
        private val COMMIT_HASH_REGEX = Regex("\\((http.+)\\).+\\(at (.+)\\)")
        private val ISSUE_KEY_REGEX = Regex("(OK-\\d+)")
        private val ISSUE_VERSION_NAME_REGEX = Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+)")
    }

    private val webVersions = versios.filter {
        it.name?.contains("Web ") ?: false
    }

    suspend fun getWorkingVersions(): List<Version> {
        return ArrayList<Version>().apply {
            add(getReleaseWorkingVersion())
            addAll(getQaOrHotfixWorkingVersions())
        }
    }

    private fun getReleaseWorkingVersion(): Version {
        return webVersions
            .filter { it.released }
            .maxBy { getVersionNameValue(it) }
            ?.copy()
            ?.also { it.name = "Web [${Server.PRODUCTION.domain}](${it.name})" }
            ?: throw Exception("release working version not found.")
    }

    private fun getVersionNameValue(version: Version): Int {
        return version.name
            ?.let { Regex("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)").find(it)?.groupValues }
            ?.let { groupValues -> groupValues[1].toInt() * 1000000 + groupValues[2].toInt() * 10000 + groupValues[3].toInt() * 100 + groupValues[4].toInt() }
            ?: Int.MAX_VALUE
    }

    private suspend fun getQaOrHotfixWorkingVersions(): List<Version> {
        return getCommitHashsText()
            .let { getCommitHashs(it) }
            .mapNotNull { commitHash ->
                getCommitMessage(commitHash.hash)
                    .let { findIssueKey(it) }
                    ?.let { getIssue(it) }
                    ?.takeIf { it.isWebIssue() }
                    ?.let { it.getVersionName() }
                    ?.let { webVersions.getWorkingVersion(it) }
                    ?.copy()
                    ?.also { it.name = "Web [${commitHash.serverDomain}](${it.name})" }
            }
    }

    private fun getCommitHashsText(): String {
        val request = Request.Builder()
            .url(AwsSecrets.WEB_SERVER_COMMIT_HASHS_URL)
            .build()
        return OkHttpClient().newCall(request).execute()
            .body()!!.string()
    }

    private fun getCommitHashs(commitHashsText: String): List<Commit> {
        return COMMIT_HASH_REGEX.findAll(commitHashsText).toList()
            .map { Commit(it.groupValues[1], it.groupValues[2]) }
    }

    private suspend fun getCommitMessage(hash: String): String {
        return GithubApiRequester.get<CommitResponseBody>(getCommitUrl(hash)).commit.message
    }

    private fun getCommitUrl(hash: String): String {
        return "https://api.github.com/repos/${GithubSecrets.GIT_AUTHOR}/${GithubSecrets.WEB_REPO_NAME}/commits/$hash"
    }

    private fun findIssueKey(commitMessage: String): String? {
        return ISSUE_KEY_REGEX.findAll(commitMessage)
            .lastOrNull()
            ?.groupValues
            ?.get(1)
    }

    private suspend fun getIssue(issueKey: String): IssueResponseBody {
        return JiraApiRequester.get(getIssueUrl(issueKey))
    }

    private fun getIssueUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey"
    }

    private fun IssueResponseBody.isWebIssue(): Boolean {
        return fields.components.firstOrNull()?.name == "Web"
    }

    private fun IssueResponseBody.getVersionName(): String? {
        return fields.fixVersions.firstOrNull()?.name?.let {
            ISSUE_VERSION_NAME_REGEX.find(it)?.groupValues?.get(1)
        }
    }
}