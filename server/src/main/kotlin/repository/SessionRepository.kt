package repository

import kotlinx.coroutines.flow.Flow
import model.RunnerEvent
import model.SessionData

interface SessionRepository {
    val sessions: MutableMap<String, SessionData>
    suspend fun addQueue(identifier: String, src: ByteArray, input: ByteArray?): SessionData?
    suspend fun run(sessionId: String): Flow<RunnerEvent>
    fun clean(sessionId: String)
}
