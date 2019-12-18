package routes.members.utils

import db.members.Member
import routes.members.bodies.MembersResponseBody
import utils.SlackApiRequester

object MembersGetter {

    private const val MEMBERS_URL = "https://slack.com/api/users.list"

    suspend fun get(): List<Member> {
        return SlackApiRequester.get<MembersResponseBody>(MEMBERS_URL).members
    }
}