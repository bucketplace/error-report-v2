package routes.versions.utils

import db.versions.Version
import enums.Platform

class ServerVersionsGetter(versios: List<Version>) {

    companion object {
        val DISPLAY_NAME = "${Platform.SERVER.shortName} 최신버전"
    }

    private val serverVersions = versios.filter {
        it.name?.contains("Server ") ?: false
    }

    fun getLatestVersions(): List<Version> {
        return listOf(
            serverVersions[0].also { it.name = DISPLAY_NAME }
        )
    }
}