package routes.home.utils

import routes.home.HomeUpdatingProcessor.RewardMember
import secrets.JiraSecrets
import utils.SlackJsonCreator.createDivider
import utils.SlackJsonCreator.createMarkdownText
import utils.SlackJsonCreator.createPlainText
import utils.toJson
import java.text.DecimalFormat
import java.util.*

class HomeJsonCreator(
    private val rewardMembers: List<RewardMember>,
    private val memberId: String
) {

    companion object {
        private val ISSUE_KEY_REGEX = Regex("(OK-\\d+)")
    }

    private val numberFormat = DecimalFormat("#,###")

    fun create() = """
        {
            "user_id": "$memberId",
            "view": ${createView()} 
        }
    """.toJson()

    private fun createView() = """
        {
            "type": "home",
            "blocks": [
                ${createHeaderSection()},
                ${createDivider()},
                ${createTopRankSections()},
                ${createDivider()},
                ${createRankSections()}
            ]
        }
    """

    private fun createHeaderSection() = """
        {
			"type": "section",
            "text": ${createMarkdownText(getThisMonth())}
		}
    """

    private fun getThisMonth(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("KST")).apply {
            this.add(Calendar.DATE, -10)
        }
        val month = calendar.get(Calendar.MONTH) + 1
        return "${month}월 리워드 랭킹"
    }

    private fun createTopRankSections(): String {
        return rewardMembers.take(3)
            .mapIndexed { index, rewardMember ->
                createTopRankSection(index + 1, rewardMember)
            }
            .joinToString(",")
    }

    private fun createTopRankSection(rank: Int, rewardMember: RewardMember) = """
        {
			"type": "section",
            "text": ${createMarkdownText(getTopRankInfo(rank, rewardMember))}
		}
    """

    private fun getTopRankInfo(rank: Int, rewardMember: RewardMember): String {
        return rewardMember.let {
            "*${rank}위 ${it.member.profile?.displayName} ${numberFormat.format(it.reward)}* (${convertIssueKeyToIssueLink(it.description)})"
        }
    }

    private fun convertIssueKeyToIssueLink(description: String): String {
        return description.replace(ISSUE_KEY_REGEX) {
            "<${JiraSecrets.DOMAIN}/browse/${it.groupValues[1]}|${it.groupValues[1]}>"
        }
    }

    private fun createRankSections(): String {
        return rewardMembers.takeLast(rewardMembers.size - 3)
            .map { createRankSection(it) }
            .joinToString(",")
    }

    private fun createRankSection(rewardMember: RewardMember) = """
        {
			"type": "section",
            "text": ${createMarkdownText(getRankInfo(rewardMember))}
		}
    """

    private fun getRankInfo(rewardMember: RewardMember): String {
        return rewardMember.let {
            "${it.member.profile?.displayName} ${numberFormat.format(it.reward)} (${convertIssueKeyToIssueLink(it.description)})"
        }
    }
}