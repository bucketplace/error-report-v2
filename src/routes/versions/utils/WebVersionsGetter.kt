package routes.versions.utils

import db.versions.Version
import enums.Platform
import enums.Server.PRODUCTION
import routes.versions.responses.CommitGettingResponseBody
import routes.versions.responses.IssueGettingResponseBody
import secrets.AwsSecrets
import secrets.GithubSecrets
import secrets.JiraSecrets
import utils.ApiRequester
import utils.GithubApiRequester
import utils.JiraApiRequester

@Suppress("ComplexRedundantLet", "SimpleRedundantLet")
class WebVersionsGetter(versios: List<Version>) {

    companion object {
        private val COMMIT_HASH_REGEX = Regex("\\((http.+)\\).+\\(at (.+)\\)")
        private val ISSUE_KEY_REGEX = Regex("(OK-\\d+)")
        private val ISSUE_VERSION_NAME_REGEX = Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+)")
    }

    private data class Commit(
        val serverDomain: String,
        val hash: String
    )

    private val webVersions = versios.filter {
        it.name?.contains("Web ") ?: false
    }

    suspend fun getLatestVersions(): List<Version> {
        return mutableListOf<Version>().apply {
            add(getLatestReleaseVersion())
            addAll(getLatestQaOrHotfixVersions())
        }
    }

    private fun getLatestReleaseVersion(): Version {
        return webVersions
            .filter { it.released }
            .maxBy { getVersionNameValue(it) }
            ?.copy()
            ?.also { it.name = getReleaseName(it) }
            ?: throw Exception("latest release version not found.")
    }

    private fun getVersionNameValue(version: Version): Int {
        return version.name
            ?.let { Regex("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)").find(it)?.groupValues }
            ?.let { groupValues -> groupValues[1].toInt() * 1000000 + groupValues[2].toInt() * 10000 + groupValues[3].toInt() * 100 + groupValues[4].toInt() }
            ?: Int.MIN_VALUE
    }

    private fun getReleaseName(version: Version): String {
        return "${Platform.WEB.shortName} [${PRODUCTION.domain}](${version.name})"
    }

    private suspend fun getLatestQaOrHotfixVersions(): List<Version> {
        return getCommitHashsText()
            .let { getCommitHashs(it) }
            .mapNotNull { commitHash ->
                getCommitMessage(commitHash.hash)
                    .let { findIssueKey(it) }
                    ?.let { getIssue(it) }
                    ?.takeIf { it.isWebIssue() }
                    ?.let { it.getVersionName() }
                    ?.let { webVersions.getLatestVersion(it) }
                    ?.copy()
                    ?.also { it.name = getQaOrHotfixName(commitHash, it) }
            }
    }

    private suspend fun getCommitHashsText(): String {
        return ApiRequester.request<String>("GET", AwsSecrets.WEB_SERVER_COMMIT_HASHS_URL)
    }

    private fun getCommitHashs(commitHashsText: String): List<Commit> {
        return COMMIT_HASH_REGEX.findAll(commitHashsText).toList()
            .map { Commit(it.groupValues[1], it.groupValues[2]) }
    }

    private suspend fun getCommitMessage(hash: String): String {
        return GithubApiRequester.get<CommitGettingResponseBody>(getCommitUrl(hash)).commit.message
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

    private suspend fun getIssue(issueKey: String): IssueGettingResponseBody {
        return JiraApiRequester.get(getIssueUrl(issueKey))
    }

    private fun getIssueUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/rest/api/2/issue/$issueKey"
    }

    private fun IssueGettingResponseBody.isWebIssue(): Boolean {
        return fields.components.any { it.name == "Web" }
    }

    private fun IssueGettingResponseBody.getVersionName(): String? {
        return fields.fixVersions.firstOrNull()?.name?.let {
            ISSUE_VERSION_NAME_REGEX.find(it)?.groupValues?.get(1)
        }
    }

    private fun getQaOrHotfixName(commitHash: Commit, version: Version): String {
        return "${Platform.WEB.shortName} [${commitHash.serverDomain}](${version.name})"
    }
}