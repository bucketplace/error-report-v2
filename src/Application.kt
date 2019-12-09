import com.google.gson.FieldNamingPolicy
import db.members.MemberDao
import db.versions.VersionDao
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.path
import io.ktor.routing.routing
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import routes.actions.actions
import routes.commands.commands
import routes.interactions.interactions
import routes.members.utils.MembersGetter
import routes.members.members
import routes.versions.utils.VersionsGetter
import routes.versions.versions


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        gson {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        }
    }

    runBlocking {
        VersionDao().clearAndInsertVersions(VersionsGetter.getWorkingVersions())
        MemberDao().clearAndInsertMembers(MembersGetter.get())
    }

    routing {
        actions()
        commands()
        interactions()
        versions()
        members()
    }
}