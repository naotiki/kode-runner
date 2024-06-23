package repository.impl

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import model.RunPhase
import model.RunnerError
import model.RunnerEvent
import model.SessionData
import repository.ConfigurationRepository
import repository.DockerRepository
import repository.RuntimeRepository
import repository.SessionRepository
import java.io.File
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

const val COMPILE_TIMEOUT_MILLIS: Long = 120 * 1000

const val EXEC_TIMEOUT_MILLIS: Long = 10 * 1000
const val INPUT_FILE = "input"

class SessionRepositoryImpl(
    private val directory: File,
    private val runtimeRepository: RuntimeRepository,
    private val dockerRepository: DockerRepository,
    private val configRepo: ConfigurationRepository
) :
    SessionRepository {
    val logger = KotlinLogging.logger { }
    override val sessions: MutableMap<String, SessionData> = Collections.synchronizedMap(LinkedHashMap())
    override fun clean(sessionId: String) {
        sessions[sessionId]?.run {
            sessionDir.deleteRecursively()
        }
        logger.trace { "clean session $sessionId" }
    }

    private suspend fun runPhase(
        containerId: String,
        phase: RunPhase,
        command: Array<String>,

        timeout: Long?,
        inputFile: File? = null
    ): Flow<RunnerEvent> {
        val logId = AtomicInteger(0)
        return flow {
            var execId: String? = null
            emitAll(callbackFlow {
                send(RunnerEvent.Start(phase))

                execId = dockerRepository.execCmdContainer(
                    containerId,
                    command,
                    object : Adapter<Frame>() {
                        override fun onNext(frame: Frame) {
                            super.onNext(frame)
                            when (frame.streamType) {
                                StreamType.STDOUT -> trySend(
                                    RunnerEvent.Log(
                                        phase,
                                        frame.payload.decodeToString(),
                                        logId.getAndIncrement()
                                    )
                                )

                                StreamType.STDERR -> trySend(
                                    RunnerEvent.ErrorLog(
                                        phase,
                                        frame.payload.decodeToString(),
                                        logId.getAndIncrement()
                                    )
                                )

                                else -> {
                                    trySend(
                                        RunnerEvent.Log(
                                            phase,
                                            frame.payload.decodeToString(),
                                            logId.getAndIncrement()
                                        )
                                    )
                                }
                            }

                        }

                        override fun onError(throwable: Throwable) {
                            super.onError(throwable)
                            throwable.printStackTrace()
                            throw throwable
                        }

                        override fun onComplete() {
                            super.onComplete()
                            channel.close()
                        }
                    }, inputFile
                ).first

                /*timeout?.let {
                    withTimeoutOrNull(it) {
                        awaitClose {
                            adapter.close()
                        }
                    } ?: run {
                        cancel(CancellationException("Docker Exec Error", RunnerError.Timeout(phase)))
                    }
                } ?:*/
                val job = timeout?.let {
                    launch {
                        delay(it)
                        throw RunnerError.Timeout(phase)
                    }
                }
                awaitClose()
                job?.cancel()
            })
            requireNotNull(execId)
            val exitCode = dockerRepository.inspectExitCodeExec(execId!!)
            if (exitCode != 0L) {
                throw RunnerError.CmdError(
                    phase,
                    "${command.joinToString(" ")} が終了コード $exitCode で終了しました"
                )
            }
            emit(RunnerEvent.Finish(phase))
        }

    }

    override suspend fun run(sessionId: String): Flow<RunnerEvent> {
        val config = configRepo.get()
        val sessionData = sessions[sessionId] ?: throw IllegalArgumentException()
        val (_, sessionDir, _, containerRuntime, inputFile) = sessionData

        val containerId = dockerRepository.prepareContainer(containerRuntime.id, sessionDir)
        sessions[sessionId] = sessionData.copy(containerId = containerId)
        val (_, compile, execute) = containerRuntime.runtimeData.commands
        return flow {
            runCatching {
                if (compile != null) {
                    this.emitAll(
                        runPhase(
                            containerId,
                            RunPhase.Compile,
                            compile.split(" ").toTypedArray(),
                            config.session.compileMillis,
                        )
                    )
                }
                emitAll(
                    runPhase(
                        containerId,
                        RunPhase.Execute,
                        execute.split(" ").toTypedArray(),
                        config.session.executeMillis,
                        inputFile
                    )
                )
            }.onFailure {
                if (it is RunnerError){
                    emit(RunnerEvent.Abort(it.phase,it))
                }
            }
        }.catch {
            logger.error { it }
            throw it
        }.onCompletion {
            withContext(Dispatchers.IO) {
                launch {
                    dockerRepository.cleanup(containerId)
                }
            }
        }
    }

    init {
        directory.mkdir()
    }

    override suspend fun addQueue(identifier: String, src: ByteArray, input: ByteArray?): SessionData? {
        logger.trace { "Add queue $identifier" }
        val runtime = runtimeRepository.searchContainerRuntime(identifier) ?: return null

        val sessionId = NanoIdUtils.randomNanoId()
        val sessionDir = directory.resolve(sessionId)
        if (!sessionDir.mkdir()) throw FileSystemException(sessionDir, reason = "mkdir failed")
        val sourceFile = sessionDir.resolve(runtime.runtimeData.sourcefileName)
        if (!withContext(Dispatchers.IO) {
                sourceFile.createNewFile()
            }) throw FileSystemException(sourceFile, reason = "createSource failed")
        sourceFile.writeBytes(src)

        val inputFile = input?.let {
            sessionDir.resolve(INPUT_FILE).apply {
                if (!withContext(Dispatchers.IO) {
                        createNewFile()
                    }) throw FileSystemException(sourceFile, reason = "createInput failed")
                writeBytes(it)
            }
        }

        val sessionData = SessionData(
            sessionId,
            sessionDir,
            sourceFile,
            runtime,
            inputFile
        )
        sessions[sessionId] = sessionData
        logger.trace { "Add queue: session $sessionId" }
        return sessionData
    }
}

private suspend fun <T> mayWithTimeout(timeout: Long?, block: suspend CoroutineScope.() -> T) = timeout?.let {
    withTimeout(it, block)
} ?: coroutineScope { block() }
