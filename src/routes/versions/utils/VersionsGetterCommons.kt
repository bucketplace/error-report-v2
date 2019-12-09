package routes.versions.utils

import db.versions.Version

private val VERSION_NAME_REGEX = Regex("(\\d+\\.\\d+(\\.\\d+)?(\\.\\d+)?)")

fun List<Version>.getWorkingVersion(versionName: String): Version? { // 핫픽스 버전은 지라에 등록되지 않기도 해서 null을 고려함.
    return this.find { it.getVersionName() == versionName }
}

private fun Version.getVersionName(): String? {
    return VERSION_NAME_REGEX.find(this.name!!)?.groupValues?.get(1)
}