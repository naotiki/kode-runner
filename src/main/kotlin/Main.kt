import di.appModule
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import org.koin.ktor.plugin.Koin
import route.appRoute


fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
private fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(Koin) {
        modules(appModule)
    }
    install(WebSockets) {
        contentConverter= KotlinxWebsocketSerializationConverter(Cbor)
    }
    routing {
        swaggerUI("swagger")
        appRoute()
    }
}
