import di.appModule
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.*
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.ktor.plugin.Koin
import route.appRoute
import java.util.zip.Deflater


suspend fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(true)
 //   socketServer("127.0.0.1",9090)
}

suspend fun socketServer(host: String, port: Int) = coroutineScope {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val serverSocket = aSocket(selectorManager).tcp().bind(host,port)
    println("SocketServer is listening at ${serverSocket.localAddress}")
    while (true){
        val socket = serverSocket.accept()
        println("Add Socket Connection $socket")
        launch {
            val receiveChannel = socket.openReadChannel()
            //receiveChannel.readUntilDelimiter()
        }
    }

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
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
    }
    routing {
        swaggerUI("swagger")
        appRoute()
    }
}
