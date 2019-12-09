package routes.members

import db.members.MemberDao
import io.ktor.application.ApplicationCall
import routes.members.utils.MembersGetter
import utils.RequestProcessor

class MembersUpdatingProcessor(call: ApplicationCall) : RequestProcessor(call) {

    override suspend fun process() {
        MemberDao().clearAndInsertMembers(MembersGetter.get())
    }
}