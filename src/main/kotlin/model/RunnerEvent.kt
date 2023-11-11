package model

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

enum class RunPhase{
    Prepare,
    Compile,
    Execute
}

@Serializable
sealed interface RunnerEvent {
    val phase:RunPhase
    @Serializable
    data class Start(override val phase: RunPhase):RunnerEvent
    @Serializable
    data class Log(override val phase: RunPhase, val data:String):RunnerEvent

    @Serializable
    data class ErrorLog(override val phase: RunPhase, val data:String):RunnerEvent
    @Serializable
    data class Finish(override val phase: RunPhase):RunnerEvent

    @Serializable
    data class Abort(override val phase: RunPhase,val error:RunnerError):RunnerEvent
}

@Serializable
sealed class RunnerError() : Throwable() {
    abstract val phase: RunPhase
    @Serializable
    data class Timeout(override val phase: RunPhase) : RunnerError()
    @Serializable
    data class CmdError(override val phase: RunPhase, val reason:String) : RunnerError()
}
