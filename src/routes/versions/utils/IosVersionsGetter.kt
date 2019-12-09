package routes.versions.utils

import db.versions.Version
import routes.versions.bodies.VersionsResponseBody
import routes.versions.bodies.VersionsResponseBody.Data
import secrets.GithubSecrets
import utils.AppStoreApiRequester
import utils.GithubApiRequester
import utils.parseJson

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

    fun getWorkingVersions(): List<Version> {
        return listOfNotNull(
            getWorkingReleaseVersion(),
            getWorkingQaVersion(),
            getWorkingHotfixVersion()
        )
    }

    private fun getWorkingReleaseVersion(): Version? {
        return getMasterBranchProjectPbxprojFileContent()
            .let { getVersionName(it) }
            .let { iosVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "iOS 마켓출시버전(${it.name})" }
    }

    private fun getMasterBranchProjectPbxprojFileContent(): String {
        return GithubApiRequester.get(MASTER_BRANCH_PROJECT_PBXPROJ_URL).body()?.string()
            ?: throw Exception("project pbxproj text not found.")
    }

    private fun getVersionName(projectPbxprojfileContent: String): String {
        return PROJECT_PBXPROJ_VERSION_NAME_REGEX.find(projectPbxprojfileContent)?.groupValues?.get(1)
            ?: throw Exception("project pbxproj versionName not found.")
    }

    private fun getWorkingQaVersion(): Version? {
        return getTestFlightVersions()
            .let { it.getWorkingTestFlightVersion(TEST_FLIGHT_QA_VERSION_NAME_REGEX) }
            .let { it.getTestFlightVersionName(TEST_FLIGHT_QA_VERSION_NAME_REGEX)!! }
            .let { iosVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "iOS QA중(정규)버전(${it.name})" }
    }

    private fun getTestFlightVersions(): List<Data> {
        return AppStoreApiRequester.get(TEST_FLIGHT_VERSIONS_URL).body()?.string()
            ?.let { json -> json.parseJson<VersionsResponseBody>().data }
            ?: throw Exception("test flight versions not found.")
    }

    private fun List<Data>.getWorkingTestFlightVersion(versionNameRegex: Regex): Data {
        return this.find { it.getTestFlightVersionName(versionNameRegex)?.isNotBlank() ?: false }
            ?: throw Exception("working test flight version not found.")
    }

    private fun Data.getTestFlightVersionName(versionNameRegex: Regex): String? {
        return versionNameRegex.find(this.attributes.version)?.groupValues?.get(1)
    }

    private fun getWorkingHotfixVersion(): Version? {
        return getTestFlightVersions()
            .let { it.getWorkingTestFlightVersion(TEST_FLIGHT_HOTFIX_VERSION_NAME_REGEX) }
            .let { it.getTestFlightVersionName(TEST_FLIGHT_HOTFIX_VERSION_NAME_REGEX) }
            ?.let { iosVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "iOS QA중(핫픽스)버전(${it.name})" }
    }
}