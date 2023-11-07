package repository.impl

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import model.RunPhase
import model.RunnerError
import model.RunnerEvent
import model.SessionData
import repository.DockerRepository
import repository.RuntimeRepository
import repository.SessionRepository
import java.io.File
import java.util.*

const val COMPILE_TIMEOUT_MILLIS: Long = 120 * 1000

const val EXEC_TIMEOUT_MILLIS: Long = 10 * 1000

class SessionRepositoryImpl(
    private val directory: File,
    private val runtimeRepository: RuntimeRepository,
    private val dockerRepository: DockerRepository
) :
    SessionRepository {
    override val sessions: MutableMap<String, SessionData> = Collections.synchronizedMap(LinkedHashMap())
    override fun clean(sessionId: String) {
        sessions[sessionId]?.sessionDir?.delete()
    }

    private suspend fun runPhase(
        containerId: String,
        phase: RunPhase,
        command: Array<String>,
        onEvent: suspend (RunnerEvent) -> Unit,
        timeout: Long?
    ) = coroutineScope{
        onEvent(RunnerEvent.Start(phase))
        val timer = timeout?.let {
            launch {
                delay(it)
                dockerRepository.cleanup(containerId)
                sessions.clear()
                throw RunnerError.Timeout(phase)
            }
        }
        dockerRepository.execCmdContainer(
            containerId,
            command,
            object : Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    super.onNext(frame)
                    runBlocking {
                        when (frame.streamType) {
                            StreamType.STDOUT -> onEvent(RunnerEvent.Log(phase, frame.payload.decodeToString()))
                            StreamType.STDERR -> onEvent(RunnerEvent.ErrorLog(phase, frame.payload.decodeToString()))
                            else -> {
                                onEvent(RunnerEvent.Log(phase, frame.payload.decodeToString()))
                            }
                        }
                    }

                }

                override fun onError(throwable: Throwable) {
                    super.onError(throwable)
                    timer?.cancel()
                    throw RunnerError.CmdError(phase, throwable.localizedMessage)
                }

                override fun onComplete() {
                    super.onComplete()
                    timer?.cancel()
                }
            }).awaitCompletion()
        onEvent(RunnerEvent.Finish(phase))
    }

    override suspend fun run(sessionId: String, onEvent: suspend (RunnerEvent) -> Unit) {
        val (_, sessionDir, _, containerRuntime) = sessions[sessionId] ?: throw IllegalArgumentException()

        val containerId = dockerRepository.prepareContainer(containerRuntime.id, sessionDir)
        val (prepare, compile, execute) = containerRuntime.runtimeData.commands
        try {
            if (compile != null) {
                runPhase(
                    containerId,
                    RunPhase.Compile,
                    compile.split(" ").toTypedArray(),
                    onEvent,
                    COMPILE_TIMEOUT_MILLIS
                )
            }
            runPhase(containerId, RunPhase.Execute, execute.split(" ").toTypedArray(), onEvent, EXEC_TIMEOUT_MILLIS)
        } finally {
            dockerRepository.cleanup(containerId)
        }
    }

    init {
        directory.mkdir()
    }

    override suspend fun addQueue(identifier: String, srcReadChannel: ByteReadChannel): SessionData? {
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
            srcReadChannel.copyTo(sourceFile.writeChannel())

            sessionData = SessionData(
                sessionId,
                sessionDir,
                sourceFile,
                runtime
            )
            sessions[sessionId] = sessionData
        }
        return sessionData
    }
}
