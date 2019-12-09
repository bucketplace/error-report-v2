package routes.members

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.members() {
    get("/members/update") { MembersUpdatingProcessor(call).process() }
}