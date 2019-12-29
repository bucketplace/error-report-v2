package routes.home

import db.members.Member
import db.members.MemberDao
import enums.Channel
import enums.Developer
import enums.QaPerson
import io.ktor.application.ApplicationCall
import routes.home.responses.SearchingResponseBody
import routes.home.utils.HomeJsonCreator
import secrets.SlackSecrets
import utils.RequestProcessor
import utils.SlackApiRequester
import java.net.URLEncoder
import java.util.*

class HomeUpdatingProcessor(call: ApplicationCall) : RequestProcessor(call) {

    companion object {
        private const val SLACK_API_DOMAIN = "https://slack.com/api"
        private const val VIEW_PUBLISH_URL = "$SLACK_API_DOMAIN/views.publish"
        private const val MESSAGE_SEARCHING_URL = "$SLACK_API_DOMAIN/search.messages"
        private val REPORTER_MEMBER_ID_REGEX = Regex("보고자.+?<@([a-zA-Z0-9]+)>")
        private val ISSUE_KEY_REGEX = Regex("지라 링크.+?http://jira.dailyhou.se/browse/(OK-\\d+)")
        private val VERSION_REGEX = Regex("발생 버전(.+?)서버")
    }

    data class ReportMessageData(val reporterMemberId: String, val issueKey: String)
    data class RewardMember(val member: Member, var reward: Int = 0, var description: String = "")

    private val memberDao = MemberDao()

    override suspend fun process() {
        publishMonthlyRewardsToHome()
    }

    private suspend fun publishMonthlyRewardsToHome() {
        val rewardMembers = getAllChannelsSortedMonthlyRewardMembers()
        memberDao.getMembers().forEach {
            val json = HomeJsonCreator(rewardMembers, it.id!!).create()
            SlackApiRequester.post<Unit>(VIEW_PUBLISH_URL, json)
        }
    }

    private suspend fun getAllChannelsSortedMonthlyRewardMembers(): List<RewardMember> {
        return listOf(
            getMonthlyRewardMembers(Channel.APP_RELEASE, 500),
            getMonthlyRewardMembers(Channel.WEB_RELEASE, 500),
            getMonthlyRewardMembers(Channel.QA, 500),
            getMonthlyRewardMembers(Channel.BETA, 1000)
        )
            .flatten()
            .sumRewardsAndDescriptionsByMember()
            .sortedByDescending { it.reward }
    }

    private suspend fun getMonthlyRewardMembers(channel: Channel, reward: Int): List<RewardMember> {
        return getMonthlyReportMessages(channel)
            .mapNotNull { reportMessage ->
                findData(reportMessage)
                    ?.let { RewardMember(memberDao.getMember(it.reporterMemberId), reward, it.issueKey) }
                    ?.takeIf { QaPerson.findByNickname(it.getNickname()) == null }
                    ?.takeIf { !isDeveloperSelfReporting(it, reportMessage) }
            }
    }

    private suspend fun getMonthlyReportMessages(channel: Channel): List<String> {
        val messages = mutableListOf<String>()
        var page = 0
        do {
            page++
            val response = SlackApiRequester.get<SearchingResponseBody>(
                url = getMessageSearchingUrl(channel, page),
                accessToken = SlackSecrets.APP_ACCESS_TOKEN
            )
            messages.addAll(response.getStringMessages())
        } while (response.hasNextPage())
        return messages
    }

    private fun getMessageSearchingUrl(channel: Channel, page: Int): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("KST")).apply {
            this.add(Calendar.DATE, -1)
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val query = URLEncoder.encode("in:#${channel.displayName} 보고자 after:$year-$month-1 before:$year-$month-$lastDay", "utf-8")
        return "$MESSAGE_SEARCHING_URL?query=$query&page=$page&count=100"
    }

    private fun SearchingResponseBody.getStringMessages(): List<String> {
        return messages.matches.mapNotNull {
            it.blocks?.getOrNull(1)?.text?.text
        }
    }

    private fun SearchingResponseBody.hasNextPage(): Boolean {
        return messages.paging.page < messages.paging.pages
    }

    private fun findData(reportMessage: String): ReportMessageData? {
        val reporterMemberId = findReporterMemberId(reportMessage)
        val issueKey = findIssueKey(reportMessage)
        return if (reporterMemberId != null && issueKey != null) {
            ReportMessageData(reporterMemberId, issueKey)
        } else {
            null
        }
    }

    private fun getOneLineMessage(reportMessage: String): String {
        return reportMessage.replace("\n", " ")
    }

    private fun findReporterMemberId(reportMessage: String): String? {
        return REPORTER_MEMBER_ID_REGEX.find(getOneLineMessage(reportMessage))?.groupValues?.get(1)
    }

    private fun findIssueKey(reportMessage: String): String? {
        return ISSUE_KEY_REGEX.find(getOneLineMessage(reportMessage))?.groupValues?.get(1)
    }

    private fun RewardMember.getNickname(): String {
        return member.profile!!.displayName!!
    }

    private fun isDeveloperSelfReporting(rewardMember: RewardMember, reportMessage: String): Boolean {
        return Developer.findByNickname(rewardMember.getNickname())
            ?.let { developer ->
                VERSION_REGEX.find(getOneLineMessage(reportMessage))?.groupValues?.get(1)
                    ?.contains(developer.platform.shortName)
                    ?: false
            }
            ?: false
    }

    private fun List<RewardMember>.sumRewardsAndDescriptionsByMember(): List<RewardMember> {
        val distinctRewardMembers = mutableMapOf<String, RewardMember>()
        this.forEach {
            distinctRewardMembers.getOrPut(it.member.id!!, { RewardMember(it.member) }).apply {
                reward += it.reward
                description += it.description + " "
            }
        }
        return distinctRewardMembers.values.toList()
    }
}