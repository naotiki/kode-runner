package repository.impl

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
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
        onEvent: (RunnerEvent) -> Unit,
        timeout: Long?,
        inputFile: File? = null
    ) {
        var complated = false
        onEvent(RunnerEvent.Start(phase))
        val logId = AtomicInteger(0)
        val (execId, adapter) = dockerRepository.execCmdContainer(
            containerId,
            command,
            object : Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    super.onNext(frame)
                    when (frame.streamType) {
                        StreamType.STDOUT -> onEvent(
                            RunnerEvent.Log(
                                phase,
                                frame.payload.decodeToString(),
                                logId.getAndIncrement()
                            )
                        )

                        StreamType.STDERR -> onEvent(
                            RunnerEvent.ErrorLog(
                                phase,
                                frame.payload.decodeToString(),
                                logId.getAndIncrement()
                            )
                        )

                        else -> {
                            onEvent(RunnerEvent.Log(phase, frame.payload.decodeToString(), logId.getAndIncrement()))
                        }
                    }

                }

                override fun onError(throwable: Throwable) {
                    super.onError(throwable)
                    throwable.printStackTrace()
                }

                override fun onComplete() {
                    super.onComplete()
                    complated = true
                }
            }, inputFile
        )
        timeout?.let {
            if (!adapter.awaitCompletion(it, TimeUnit.MILLISECONDS)) {

                throw RunnerError.Timeout(phase)
            }
        } ?: adapter.awaitCompletion()


        val exitCode = dockerRepository.inspectExitCodeExec(execId)
        if (exitCode != 0L) {
            throw RunnerError.CmdError(phase, "${command.joinToString(" ")} が終了コード $exitCode で終了しました")
        }
        onEvent(RunnerEvent.Finish(phase))
    }

    override suspend fun run(sessionId: String, onEvent: (RunnerEvent) -> Unit) {
        val config = configRepo.get()
        val sessionData = sessions[sessionId] ?: throw IllegalArgumentException()
        val (_, sessionDir, _, containerRuntime, inputFile) = sessionData

        val containerId = dockerRepository.prepareContainer(containerRuntime.id, sessionDir)
        sessions[sessionId] = sessionData.copy(containerId = containerId)
        val (prepare, compile, execute) = containerRuntime.runtimeData.commands
        try {
            if (compile != null) {
                runPhase(
                    containerId,
                    RunPhase.Compile,
                    compile.split(" ").toTypedArray(),
                    onEvent,
                    config.session.compileMillis,
                )
            }
            runPhase(
                containerId,
                RunPhase.Execute,
                execute.split(" ").toTypedArray(),
                onEvent,
                config.session.executeMillis,
                inputFile
            )
        } catch (e: Throwable) {
            logger.error{ e }
            throw e
        } finally {
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
        val runtime = runtimeRepository.searchContainerRuntime(identifier)
        var sessionData: SessionData? = null
        if (runtime != null) {
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

            sessionData = SessionData(
                sessionId,
                sessionDir,
                sourceFile,
                runtime,
                inputFile
            )
            sessions[sessionId] = sessionData
            logger.trace { "Add queue: session $sessionId" }
        }
        return sessionData
    }
}
