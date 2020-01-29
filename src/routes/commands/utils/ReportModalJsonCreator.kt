package routes.commands.utils

import db.versions.VersionDao
import enums.*
import utils.SlackJsonCreator.createDivider
import utils.SlackJsonCreator.createInputBlock
import utils.SlackJsonCreator.createMarkdownText
import utils.SlackJsonCreator.createOption
import utils.SlackJsonCreator.createPlainText
import utils.SlackJsonCreator.createSelectBlock
import utils.toJson

object ReportModalJsonCreator {

    fun create(triggerId: String) = """
        {
            "trigger_id": "$triggerId",
            "view": ${createView()} 
        }
    """.toJson()

    private fun createView() = """
        {
            "type": "modal",
            "callback_id": "${CallbackId.CREATE_REPORT.name.toLowerCase()}",
            "title": ${createPlainText("오류리포팅 V2")},
            "submit": ${createPlainText("등록")},
            "close": ${createPlainText("취소")},
            "blocks": [
                ${createInformationSection()},
                ${createDivider()},
                ${createPathInputBlock()},
                ${createSituationInputBlock()},
                ${createExpectedResultInputBlock()},
                ${createVersionSelectBlock()},
                ${createServerSelectBlock()},
                ${createEtcEnvironmentInputBlock()},
                ${createPrioritySelectBlock()},
                ${createErrorTypeSelectBlock()},
                ${createReproducingSelectBlock()},
                ${createTrackSelectBlock()},
                ${createDeveloperSelectBlock()},
                ${createChannelSelectBlock()}
            ]
        }
    """

    private fun createInformationSection() = """
        {
			"type": "section",
			"text": ${createMarkdownText("※ *스크린샷* 은 스레드에 댓글로 달아주세요.")}
		}
    """

    private fun createPathInputBlock(): String {
        return createInputBlock(
            blockId = BlockId.PATH.name.toLowerCase(),
            label = createPlainText("발생 경로"),
            placeholder = createPlainText(" "),
            hint = createPlainText("예) 제품 디테일›리뷰목록 전체보기›리뷰 쓰기"),
            optional = false
        )
    }

    private fun createSituationInputBlock(): String {
        return createInputBlock(
            blockId = BlockId.SITUATION.name.toLowerCase(),
            label = createPlainText("오류 현상"),
            placeholder = createPlainText(" "),
            hint = createPlainText("예) 포토리뷰 작성 시 마지막 단계에서 리뷰 작성이 불가합니다."),
            optional = false
        )
    }

    private fun createExpectedResultInputBlock(): String {
        return createInputBlock(
            blockId = BlockId.EXPECTED_RESULT.name.toLowerCase(),
            label = createPlainText("기대 결과"),
            placeholder = createPlainText(" "),
            hint = createPlainText("예) 리뷰가 적상적으로 작성되어야 합니다."),
            optional = true
        )
    }

    private fun createVersionSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.VERSION.name.toLowerCase(),
            label = createPlainText("발생 버전"),
            placeholder = createPlainText("선택"),
            options = createVersionOptions(),
            optional = false
        )
    }

    private fun createVersionOptions(): String {
        return VersionDao().getVersions()
            .map { version -> createOption(version.name!!, version.name!!) }
            .toString()
    }

    private fun createServerSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.SERVER.name.toLowerCase(),
            label = createPlainText("서버"),
            placeholder = createPlainText("선택"),
            options = createServerOptions(),
            optional = true
        )
    }

    private fun createServerOptions(): String {
        return Server.values().toList()
            .map { server -> createOption(server.displayName, server.displayName) }
            .toString()
    }

    private fun createEtcEnvironmentInputBlock(): String {
        return createInputBlock(
            blockId = BlockId.ETC_ENVIRONMENT.name.toLowerCase(),
            label = createPlainText("기타 환경"),
            placeholder = createPlainText(" "),
            hint = createPlainText("예) 아이폰XR+ / 소프트웨어 13.2.2 / 사파리 9.1.2"),
            optional = true
        )
    }

    private fun createPrioritySelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.PRIORITY.name.toLowerCase(),
            label = createPlainText("심각도"),
            placeholder = createPlainText("선택"),
            options = createPriorityOptions(),
            optional = false
        )
    }

    private fun createPriorityOptions(): String {
        return Priority.values().toList()
            .map { priority -> createOption(priority.displayName, priority.displayName) }
            .toString()
    }

    private fun createErrorTypeSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.ERROR_TYPE.name.toLowerCase(),
            label = createPlainText("예상 오류 유형"),
            placeholder = createPlainText("선택"),
            options = createErrorTypeOptions(),
            optional = true
        )
    }

    private fun createErrorTypeOptions(): String {
        return ErrorType.values().toList()
            .map { priority -> createOption(priority.displayName, priority.displayName) }
            .toString()
    }

    private fun createReproducingSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.REPRODUCING.name.toLowerCase(),
            label = createPlainText("재현 가능 여부"),
            placeholder = createPlainText("선택"),
            options = createReproducingOptions(),
            optional = true
        )
    }

    private fun createReproducingOptions(): String {
        return Reproducing.values().toList()
            .map { reproducing -> createReproducingOption(reproducing) }
            .toString()
    }

    private fun createReproducingOption(reproducing: Reproducing): String {
        return createOption(reproducing.displayName, reproducing.displayName)
    }

    private fun createTrackSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.TRACK.name.toLowerCase(),
            label = createPlainText("예상 담당트랙"),
            placeholder = createPlainText("선택"),
            options = createTrackOptions(),
            optional = false
        )
    }

    private fun createTrackOptions(): String {
        return Track.values().toList()
            .map { track -> createOption(track.displayName, track.displayName) }
            .toString()
    }

    private fun createDeveloperSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.DEVELOPER.name.toLowerCase(),
            label = createPlainText("예상 담당개발자"),
            placeholder = createPlainText("선택"),
            options = createDeveloperOptions(),
            optional = false
        )
    }

    private fun createDeveloperOptions(): String {
        return Developer.values().toList()
            .map { developer -> createOption(developer.displayName, developer.displayName) }
            .toString()
    }

    private fun createChannelSelectBlock(): String {
        return createSelectBlock(
            blockId = BlockId.CHANNEL.name.toLowerCase(),
            label = createPlainText("리포팅 채널"),
            placeholder = createPlainText("선택"),
            options = createChannelOptions(),
            optional = false
        )
    }

    private fun createChannelOptions(): String {
        return Channel.values().toList()
            .map { slackChannel -> createOption(slackChannel.displayName, slackChannel.displayName) }
            .toString()
    }
}