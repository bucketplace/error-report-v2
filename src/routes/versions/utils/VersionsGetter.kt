package routes.versions.utils

import db.versions.Version
import secrets.JiraSecrets
import utils.JiraApiRequester

object VersionsGetter {

    private const val VERSIONS_URL = "${JiraSecrets.DOMAIN}/rest/api/2/project/OK/versions"

    suspend fun getWorkingVersions(): List<Version> {
        val allVersions = getAllVersions()
        return mutableListOf<Version>().apply {
            addAll(IosVersionsGetter(allVersions).getLatestVersions())
            addAll(AndroidVersionsGetter(allVersions).getLatestVersions())
            addAll(WebVersionsGetter(allVersions).getLatestVersions())
            addAll(ServerVersionsGetter(allVersions).getLatestVersions())
            addAll(AndroidProVersionsGetter(allVersions).getLatestVersions())
        }
    }

    suspend fun getAllVersions(): List<Version> {
        return JiraApiRequester.get(VERSIONS_URL)
    }
}
