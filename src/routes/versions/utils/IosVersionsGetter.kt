package routes.versions.utils

import db.versions.Version
import enums.Platform
import routes.versions.responses.VersionsGettingResponseBody
import routes.versions.responses.VersionsGettingResponseBody.Data
import secrets.GithubSecrets
import utils.AppStoreApiRequester
import utils.GithubApiRequester

@Suppress("ComplexRedundantLet", "SimpleRedundantLet")
class IosVersionsGetter(versios: List<Version>) {

    companion object {
        private const val MASTER_BRANCH_PROJECT_PBXPROJ_URL =
            "https://api.github.com/repos/${GithubSecrets.GIT_AUTHOR}/${GithubSecrets.IOS_REPO_NAME}/contents/${GithubSecrets.IOS_XCODEPROJ_NAME}/project.pbxproj?ref=master"
        private val PROJECT_PBXPROJ_VERSION_NAME_REGEX = Regex("MARKETING_VERSION = (\\d+\\.\\d+(\\.\\d+)?)")
        private const val TEST_FLIGHT_VERSIONS_URL = "https://api.appstoreconnect.apple.com/v1/preReleaseVersions"
        private val TEST_FLIGHT_QA_VERSION_NAME_REGEX = Regex("^(\\d+\\.\\d+)$")
        private val TEST_FLIGHT_HOTFIX_VERSION_NAME_REGEX = Regex("^(\\d+\\.\\d+\\.\\d+)$")
    }

    private val iosVersions = versios.filter {
        it.name?.contains("iOS ") ?: false
    }

    suspend fun getLatestVersions(): List<Version> {
        return listOfNotNull(
            getLatestReleaseVersion(),
            getLatestQaVersion(),
            getLatestHotfixVersion()
        )
    }

    private suspend fun getLatestReleaseVersion(): Version? {
        return getMasterBranchProjectPbxprojFileContent()
            .let { getVersionName(it) }
            .let { iosVersions.getLatestVersion(it) }
            ?.copy()
            ?.also { it.name = getReleaseName(it) }
    }

    private suspend fun getMasterBranchProjectPbxprojFileContent(): String {
        return GithubApiRequester.get(MASTER_BRANCH_PROJECT_PBXPROJ_URL)
    }

    private fun getVersionName(projectPbxprojfileContent: String): String {
        return PROJECT_PBXPROJ_VERSION_NAME_REGEX.find(projectPbxprojfileContent)?.groupValues?.get(1)
            ?: throw Exception("project pbxproj versionName not found.")
    }

    private fun getReleaseName(version: Version): String {
        return "${Platform.IOS.shortName} 마켓출시버전(${version.name})"
    }

    private suspend fun getLatestQaVersion(): Version? {
        return getTestFlightVersions()
            .let { it.getLatestTestFlightVersion(TEST_FLIGHT_QA_VERSION_NAME_REGEX) }
            .let { it.getTestFlightVersionName(TEST_FLIGHT_QA_VERSION_NAME_REGEX)!! }
            .let { iosVersions.getLatestVersion(it) }
            ?.copy()
            ?.also { it.name = getQaVersion(it) }
    }

    private fun getQaVersion(version: Version): String {
        return "${Platform.IOS.shortName} QA중(정규)버전(${version.name})"
    }

    private suspend fun getTestFlightVersions(): List<Data> {
        return AppStoreApiRequester.get<VersionsGettingResponseBody>(TEST_FLIGHT_VERSIONS_URL).data
    }

    private fun List<Data>.getLatestTestFlightVersion(versionNameRegex: Regex): Data {
        return this.find { it.getTestFlightVersionName(versionNameRegex)?.isNotBlank() ?: false }
            ?: throw Exception("latest test flight version not found.")
    }

    private fun Data.getTestFlightVersionName(versionNameRegex: Regex): String? {
        return versionNameRegex.find(this.attributes.version)?.groupValues?.get(1)
    }

    private suspend fun getLatestHotfixVersion(): Version? {
        return getTestFlightVersions()
            .let { it.getLatestTestFlightVersion(TEST_FLIGHT_HOTFIX_VERSION_NAME_REGEX) }
            .let { it.getTestFlightVersionName(TEST_FLIGHT_HOTFIX_VERSION_NAME_REGEX) }
            ?.let { iosVersions.getLatestVersion(it) }
            ?.copy()
            ?.also { it.name = getHotfixName(it) }
    }

    private fun getHotfixName(version: Version): String {
        return "${Platform.IOS.shortName} QA중(핫픽스)버전(${version.name})"
    }
}