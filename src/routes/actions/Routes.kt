package routes.actions

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.post

fun Route.actions() {
    post("/actions") { ActionsProcessor(call).process() }
}