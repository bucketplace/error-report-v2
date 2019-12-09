package routes.versions.utils

import db.versions.Version
import secrets.JiraSecrets
import utils.JiraApiRequester

object VersionsGetter {

    private const val VERSIONS_URL = "${JiraSecrets.DOMAIN}/rest/api/2/project/OK/versions"

    suspend fun getWorkingVersions(): List<Version> {
        val allVersions = getAllVersions()
        return ArrayList<Version>().apply {
            addAll(IosVersionsGetter(allVersions).getWorkingVersions())
            addAll(AndroidVersionsGetter(allVersions).getWorkingVersions())
            addAll(WebVersionsGetter(allVersions).getWorkingVersions())
            addAll(ServerVersionsGetter(allVersions).getWorkingVersions())
            addAll(AndroidProVersionsGetter(allVersions).getWorkingVersions())
        }
    }

    suspend fun getAllVersions(): List<Version> {
        return JiraApiRequester.get(VERSIONS_URL)
    }
}
