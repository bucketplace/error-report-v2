package routes.members.utils

import db.members.Member
import routes.members.responses.MembersGettingResponseBody
import utils.SlackApiRequester

object MembersGetter {

    private const val MEMBERS_URL = "https://slack.com/api/users.list"

    suspend fun get(): List<Member> {
        return SlackApiRequester.get<MembersGettingResponseBody>(MEMBERS_URL).members
    }
}