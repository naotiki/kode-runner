import di.appModule
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import model.Commands
import model.RuntimeData
import model.MetaData
import net.mamoe.yamlkt.Yaml
import org.koin.ktor.plugin.Koin
import route.appRoute
import java.time.Duration

/*val cr = RuntimeData(
    "kotlin",
    "Kotlin",
    "Main.kt",
    "kt",
    metaData = MetaData("1.9.10", "JVM"),
    commands = Commands(
        compile = "kotlinc -verbose src.kt -d out.jar",
        execute = "java -jar out.jar"
    )
)*/

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
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        swaggerUI("swagger")
        appRoute()
    }
}
