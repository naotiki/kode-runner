package repository.impl

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import model.*
import repository.ConfigurationRepository
import repository.DockerRepository
import repository.RuntimeRepository
import repository.SessionRepository
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

const val INPUT_FILE = "input"

class SessionRepositoryImpl(
    private val directory: File,
    private val runtimeRepository: RuntimeRepository,
    private val dockerRepository: DockerRepository,
    private val configRepo: ConfigurationRepository
) :
    SessionRepository {
    private val logger = KotlinLogging.logger { }
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
            val logFlow: Flow<RunnerEvent> = callbackFlow {
                send(RunnerEvent.Start(phase))
                val stdout = StackString("", 1000)
                val stderr = StackString("", 1000)
                execId = dockerRepository.execCmdContainer(
                    containerId,
                    command,
                    object : Adapter<Frame>() {
                        override fun onNext(frame: Frame) {
                            super.onNext(frame)
                            when (frame.streamType) {
                                StreamType.STDOUT -> stdout.push(frame.payload.decodeToString())
                                StreamType.STDERR -> stderr.push(frame.payload.decodeToString().also {
                                    println(it)
                                })
                                else -> stdout.push(frame.payload.decodeToString())
                            }
                        }

                        override fun onError(throwable: Throwable) {
                            super.onError(throwable)
                            throwable.printStackTrace()
                            throw throwable
                        }

                        override fun onComplete() {
                            super.onComplete()
                            val stdo = stdout.popAll()
                            if (stdo.isNotEmpty()) {
                                trySend(RunnerEvent.Log(phase, stdo, logId.getAndIncrement()))
                            }
                            val stde = stderr.popAll()
                            if (stde.isNotEmpty()) {
                                trySend(RunnerEvent.ErrorLog(phase, stde, logId.getAndIncrement()))
                            }
                            channel.close()
                        }
                    }, inputFile
                ).first

                val sendRoutine = launch {
                    while (true) {
                        val stdo = stdout.popAll()
                        if (stdo.isNotEmpty()) {
                            send(RunnerEvent.Log(phase, stdo, logId.getAndIncrement()))
                        }
                        val stde = stderr.popAll()
                        if (stde.isNotEmpty()) {
                            send(RunnerEvent.ErrorLog(phase, stde, logId.getAndIncrement()))
                        }
                        delay(750)
                    }
                }

                val job = timeout?.let {
                    launch {
                        delay(it)
                        println("Timeout! $it")
                        throw RunnerError.Timeout(phase)
                    }
                }
                awaitClose{
                    sendRoutine.cancel()
                }
                job?.cancel()
            }
            emitAll(logFlow)
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

        }.catch {
            logger.error { it }
            if (it is RunnerError) {
                emit(RunnerEvent.Abort(it.phase, it))
                return@catch
            }
            throw it
        }.onCompletion {
            withContext(Dispatchers.IO) {
                launch {
                    clean(sessionId)
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

