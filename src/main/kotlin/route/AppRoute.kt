package route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import model.RespondSession.Companion.toRespondSession
import model.RunnerError
import model.RunnerEvent
import org.koin.ktor.ext.inject
import repository.RuntimeRepository
import repository.SessionRepository
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

fun Routing.appRoute() {
    val runtimeRepository by inject<RuntimeRepository>()
    route("/runtimes") {
        get {
            call.respond(runtimeRepository.listRuntimes())
        }
        get("/{name}") {
            val name = call.parameters["name"]!!
            val runtime = runtimeRepository.searchRuntimeData(name) ?: kotlin.run {
                call.respond(HttpStatusCode.NotFound, "Not Found")
                return@get
            }
            call.respond(runtime)
        }
    }
    val sessionRepository by inject<SessionRepository>()
    post("/run") {
        val identifier = call.parameters["langAlias"]
        if (identifier == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val readChannel = call.receiveChannel()
        val sessionData = sessionRepository.addQueue(identifier, readChannel)
        if (sessionData == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        call.respond(sessionData.toRespondSession())
    }
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
    webSocket("/run/{sessionId}") {
        val thisConnection = Connection(this)
        connections += thisConnection
        val sessionId = call.parameters["sessionId"]
        try {
            sessionRepository.run(sessionId!!) {
                sendSerialized(it)
            }
        } catch (e: RunnerError) {
            sendSerialized(RunnerEvent.Abort(e.phase,e.localizedMessage))
            println("Error:$e")
        } catch (e: Exception) {
            println(e.localizedMessage)
        } finally {
            println("Removing $thisConnection!")
            connections -= thisConnection
            sessionRepository.clean(sessionId!!)
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val name = "session${lastId.getAndIncrement()}"
}
