package routes.members.responses

import db.members.Member

data class MembersGettingResponseBody(
    val members: List<Member>
)