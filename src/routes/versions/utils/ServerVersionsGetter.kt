package routes.versions.utils

import db.versions.Version

class ServerVersionsGetter(versios: List<Version>) {

    companion object {
        const val DISPLAY_NAME = "Server 최신버전"
    }

    private val serverVersions = versios.filter {
        it.name?.contains("Server ") ?: false
    }

    fun getWorkingVersions(): List<Version> {
        return listOf(
            serverVersions[0].also { it.name = DISPLAY_NAME }
        )
    }
}