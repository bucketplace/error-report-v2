package routes.members.bodies

import db.members.Member

data class MembersResponseBody(
    val members: List<Member>
)