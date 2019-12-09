package routes.versions

import db.versions.VersionDao
import io.ktor.application.ApplicationCall
import routes.versions.utils.VersionsGetter
import utils.RequestProcessor

class VersionsUpdatingProcessor(call: ApplicationCall) : RequestProcessor(call) {

    override suspend fun process() {
        VersionDao().clearAndInsertVersions(VersionsGetter.getWorkingVersions())
    }
}