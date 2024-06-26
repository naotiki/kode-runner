import command.builder.slash.*
import command.builder.text.textCommands
import command.slash.Info
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.rpc.client.withService
import kotlinx.rpc.serialization.cbor
import kotlinx.rpc.transport.ktor.client.installRPC
import kotlinx.rpc.transport.ktor.client.rpc
import kotlinx.rpc.transport.ktor.client.rpcConfig
import kotlinx.serialization.ExperimentalSerializationApi
import model.AppConfig
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlBuilder
import net.mamoe.yamlkt.YamlBuilder.*
import service.RunnerService
import java.io.File

val client by lazy {
    HttpClient {

        installRPC()
        defaultRequest {
            url("http://${appConfig.serverHost}")
        }
        install(ContentNegotiation) {
            json()
        }
    }
}

lateinit var runnerService: RunnerService


val slashCommands by lazy {
    arrayOf<RootCommand>(
        Info()
    )
}
private val yaml = Yaml {
    encodeDefaultValues = true
    listSerialization = ListSerialization.BLOCK_SEQUENCE
    mapSerialization = MapSerialization.BLOCK_MAP
}
val appConfig by lazy {
    val f = File("config.yml")
    if (!f.exists()) {
        f.createNewFile()
        f.writeText(yaml.encodeToString(AppConfig()))
        throw NoSuchFileException(
            f,
            reason = "The config file,config.yml not found. please fill <token> in generated config.yml"
        )
    }
    yaml.decodeFromString(AppConfig.serializer(), f.readText())
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun main(args: Array<String>) {
    runnerService = client.rpc {
        println(appConfig.serverHost)
        url("ws://${appConfig.serverHost}/rpc")
        rpcConfig {
            serialization {
                cbor()
            }
        }
    }.withService<RunnerService>()
    val env = appConfig.env
    println("Starting... on $env environment")
    val kord = Kord(appConfig.tokens.getValue(env))
    println("Register slash-commands")
    slashCommands.forEach {
        println("\tRegister ${(it as? BaseCommand)?.name}")
        it.register(kord)
    }


    kord.on<MessageCreateEvent> {
        // 他のボットを無視し、私たち自身も無視します。ここでは人間のみにサービスを提供します。
        if (message.author?.isBot != false) return@on
        val msgContent = message.content
        textCommands(msgContent)
    }
    kord.on<ChatInputCommandInteractionCreateEvent> {
        println(interaction.command)
        when (interaction.command) {
            is dev.kord.core.entity.interaction.GroupCommand -> slashCommands.filterIsInstance<SlashCommandWithGroup>()
            is dev.kord.core.entity.interaction.RootCommand -> slashCommands.filterIsInstance<SlashCommand>()
            is dev.kord.core.entity.interaction.SubCommand -> slashCommands.filterIsInstance<SlashCommandWithSub>()
        }.single { it.name == interaction.command.rootName }.exec(interaction)
    }

    println("Login")
    kord.login {
        // we need to specify this to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
