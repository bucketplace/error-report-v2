package routes.home

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.home() {
    get("/home/update") { HomeUpdatingProcessor(call).process() }
}