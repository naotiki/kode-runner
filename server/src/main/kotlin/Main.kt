import di.appModule
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.Identity.decode
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.rpc.transport.ktor.server.RPC
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import org.koin.ktor.plugin.Koin
import route.appRoute
import java.util.zip.Deflater


fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080,) {
        module()
    }.start(true)
}

private fun Application.module() {
    install(RPC)

    install(ContentNegotiation) {
        json()
    }

    install(Koin) {
        modules(appModule)
    }
    routing {
        //swaggerUI("swagger")
        appRoute()
    }
}
