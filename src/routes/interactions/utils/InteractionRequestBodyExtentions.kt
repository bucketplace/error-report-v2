package routes.interactions.utils

import enums.Channel
import enums.Channel.*
import routes.interactions.requests.InteractionRequestBody

fun InteractionRequestBody.hasTttt(): Boolean {
    return view.state.values.situation.action.value == "tttt"
}

fun InteractionRequestBody.hasReleaseReportingChannel(): Boolean {
    return Channel.get(view.state.values.channel.action.selectedOption!!.value) in listOf(APP_RELEASE, WEB_RELEASE)
}