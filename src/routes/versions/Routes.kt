package routes.versions

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.versions() {
    get("/versions/update") { VersionsUpdatingProcessor(call).process() }
}