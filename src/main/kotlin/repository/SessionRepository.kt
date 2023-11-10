package repository

import io.ktor.utils.io.*
import model.RunnerEvent
import model.SessionData

interface SessionRepository {
    val sessions: MutableMap<String, SessionData>
    suspend fun addQueue(identifier: String, srcReadChannel: ByteReadChannel): SessionData?
    suspend fun run(sessionId: String, onEvent: (RunnerEvent) -> Unit)
    fun clean(sessionId: String)
}
