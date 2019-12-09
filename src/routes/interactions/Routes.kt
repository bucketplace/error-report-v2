package routes.interactions

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.post

fun Route.interactions() {
    post("/interactions") { InteractionsProcessor(call).process() }
}