package model

import kotlinx.serialization.Serializable

@Serializable
data class RespondSession(
    val sessionId: String,
    val runtimeData: RuntimeData
){
    companion object{
        fun SessionData.toRespondSession(): RespondSession = RespondSession(sessionId,containerRuntime.runtimeData)
    }
}
