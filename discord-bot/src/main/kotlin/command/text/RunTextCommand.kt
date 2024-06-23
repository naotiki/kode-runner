package command.text

import command.builder.text.TextCommand
import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.rpc.internal.streamScoped
import model.RunPhase
import model.RunPhase.*
import model.RunnerError
import model.RunnerEvent
import runnerService
import util.escapeCodeblocks

class RunTextCommand : TextCommand("run") {
    override suspend fun MessageCreateEvent.execute(body: String) = coroutineScope {
        val result =
            "```(?<lang>(?!input).*)\\n(?<src>(?:(?!```)[\\s\\S])*)```".toRegex().find(body) ?: return@coroutineScope
        val ownMessage = message.reply { content = "お待ちください..." }
        val input = "```input\\n(?<body>[\\s\\S]*)```".toRegex().find(body)
        val inputBody = input?.groups?.get("body")?.value
        val lang = result.groups["lang"]?.value
        val src = result.groups["src"]?.value
        if (lang == null || src == null) {
            ownMessage.edit {
                content = null
                embed {
                    color = Color(0xff0000)
                    title = "コマンド解析エラー"
                    description = """正しくコマンドを読み取れませんでした。
                                |$prefix の形式を確認してください。
                            """.trimMargin()
                }
            }
            return@coroutineScope
        }
        val (sessionId, runtimeData) = runnerService.createSession(lang, src, inputBody) ?: run {
            ownMessage.edit {
                content = null
                embed {
                    color = Color(0xff0000)
                    title = "言語エラー"
                    description = """非対応の言語です
                                |$prefix の形式を確認してください。
                            """.trimMargin()
                }
            }
            return@coroutineScope
        }

        var errorLog: String? = null
        var embedColor = Color(0x0f0f0f)
        var phase: RunPhase? = Prepare
        val logList = mutableListOf<RunnerEvent.LogBase>()
        var lastUpdated = System.currentTimeMillis()
        var job: Job? = null
        suspend fun editRunEmbed() {
            job?.cancel()
            job = launch {
                ownMessage.edit {
                    content = null
                    embed {
                        title = runtimeData.name
                        color = embedColor
                        description = runtimeData.metaData.run { "$version ($processor)" }
                        field {
                            name = "フェーズ"
                            value = when (phase) {
                                Prepare -> "準備中"
                                Compile -> "コンパイル中"
                                Execute -> "実行中"
                                null -> "終了"
                            }
                        }

                        synchronized(logList) {
                            logList.filter { it.phase == Compile }.ifEmpty { null }?.let {
                                field {
                                    name = "コンパイルログ"
                                    value = """
                                        |```
                                        |${it.joinToString("") { it.data }.escapeCodeblocks().takeLast(1000)}
                                        |```
                                    """.trimMargin()
                                }
                            }
                            logList.filter { it.phase == Execute }.ifEmpty { null }?.let {
                                field {
                                    name = "実行ログ"
                                    value = """
                                        |```
                                        |${it.joinToString("") { it.data }.escapeCodeblocks().takeLast(1000)}
                                        |```
                                    """.trimMargin()
                                }
                            }
                        }
                        errorLog?.let {
                            field {
                                name = "エラー"
                                value = """
                                        |```
                                        |${it.escapeCodeblocks().takeLast(1000)}
                                        |```
                                    """.trimMargin()
                            }
                        }

                        footer {
                            text = sessionId
                        }
                    }
                }

            }
        }
        streamScoped {
            runnerService.executeSession(sessionId).collect { event ->

                when (event) {
                    is RunnerEvent.LogBase -> {
                        logList.add(event)
                        logList.sortBy { it.id }
                    }

                    is RunnerEvent.Abort -> {
                        embedColor = Color(0xaf0000)
                        val errorText = when (event.error) {
                            is RunnerError.CmdError -> """
                                        コマンドエラー
                                        ${(event.error as RunnerError.CmdError).reason}
                                        """.trimIndent()

                            is RunnerError.Timeout -> """
                                        タイムアウトエラー
                                        コマンドの実行時間を超過しました
                                        """.trimIndent()
                        }.also {
                            """
                                    フェーズ ${event.phase.name}
                                    $it
                                    """.trimIndent()
                        }
                        errorLog = errorLog?.plus(errorText) ?: errorText
                    }

                    is RunnerEvent.Start -> {
                        phase = event.phase
                        embedColor = Color(0x0000af)
                    }

                    is RunnerEvent.Finish -> {
                        if (event.phase == Execute) embedColor = Color(0x00af00)
                    }

                    else -> println(event)
                }
                println(event)
                if (System.currentTimeMillis() - lastUpdated >= 500 || event !is RunnerEvent.LogBase) {
                    println(System.currentTimeMillis() - lastUpdated)
                    kotlin.runCatching {
                        editRunEmbed()
                    }.onFailure {
                        it.printStackTrace()
                    }

                    lastUpdated = System.currentTimeMillis()
                }

            }
        }
        phase = null
        kotlin.runCatching {
            editRunEmbed()
        }.onFailure {
            it.printStackTrace()
        }

    }
}



