package repository

import model.RunnerEvent
import model.SessionData

interface SessionRepository {
    val sessions: MutableMap<String, SessionData>
    suspend fun addQueue(identifier: String, src: ByteArray, input: ByteArray?): SessionData?
    suspend fun run(sessionId: String, onEvent: (RunnerEvent) -> Unit)
    fun clean(sessionId: String)
}
