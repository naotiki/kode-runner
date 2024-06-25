package command.builder.text

import appConfig
import command.text.RunTextCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent


private val textCommandList = listOf<TextCommand>(RunTextCommand())
suspend fun MessageCreateEvent.textCommands(messageStr: String) {
    textCommandList.singleOrNull {
        messageStr.startsWith(it.prefix)
    }?.run {
        if (this@textCommands.guildId !in appConfig.allowed.guilds && message.author?.id !in appConfig.allowed.users
        ) {
            message.reply {
                content = """このサーバーでの実行は許可されていません
                    |ごめんね
                """.trimMargin()
            }
            return@run
        }
        execute(messageStr.removePrefix(prefix))
    }
}


const val textCommandPrefix = "!"

