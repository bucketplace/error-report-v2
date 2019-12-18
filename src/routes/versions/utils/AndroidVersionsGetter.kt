package routes.versions.utils

import db.versions.Version
import secrets.GithubSecrets
import utils.GithubApiRequester

@Suppress("ComplexRedundantLet", "BlockingMethodInNonBlockingContext")
class AndroidVersionsGetter(versios: List<Version>) {

    companion object {
        private const val BUILD_GRADLE_URL =
            "https://api.github.com/repos/${GithubSecrets.GIT_AUTHOR}/${GithubSecrets.ANDROID_REPO_NAME}/contents/app/build.gradle"
        private val BUILD_GRADLE_VERSION_NAME_REGEX = Regex("versionName \"(\\d+\\.\\d+\\.\\d+)\"")
    }

    private val androidVersions = versios.filter {
        it.name?.contains("And ") ?: false
    }

    suspend fun getWorkingVersions(): List<Version> {
        return listOfNotNull(
            getWorkingReleaseVersion(),
            getWorkingQaVersion(),
            getWorkingHotfixVersion()
        )
    }

    private suspend fun getWorkingReleaseVersion(): Version? {
        return getBuildGradleFileContent("master")
            .let { getVersionName(it) }
            .let { androidVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "AOS 오늘의집 마켓출시버전(${it.name})" }
    }

    private suspend fun getBuildGradleFileContent(branch: String): String {
        return GithubApiRequester.get(getBuildGradleUrl(branch))
    }

    private fun getBuildGradleUrl(branch: String): String {
        return "$BUILD_GRADLE_URL?ref=$branch"
    }

    private fun getVersionName(buildGradleFileContent: String): String {
        return BUILD_GRADLE_VERSION_NAME_REGEX.find(buildGradleFileContent)?.groupValues?.get(1)
            ?: throw Exception("build gradle versionName not found.")
    }

    private suspend fun getWorkingQaVersion(): Version? {
        return getBuildGradleFileContent("qa")
            .let { getVersionName(it) }
            .let { androidVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "AOS 오늘의집 QA중(정규)버전(${it.name})" }
    }

    private suspend fun getWorkingHotfixVersion(): Version? {
        return getBuildGradleFileContent("hotfix")
            .let { getVersionName(it) }
            .let { androidVersions.getWorkingVersion(it) }
            ?.copy()
            ?.also { it.name = "AOS 오늘의집 QA중(핫픽스)버전(${it.name})" }
    }
}