package service.impl

import kotlinx.coroutines.flow.Flow
import model.RespondSession.Companion.toRespondSession
import model.RunnerEvent
import repository.SessionRepository
import service.RunnerService
import kotlin.coroutines.CoroutineContext

class RunnerServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val sessionRepository: SessionRepository
) :
    RunnerService {
    override suspend fun executeSession(sessionId: String): Flow<RunnerEvent> = sessionRepository.run(sessionId)

    override suspend fun createSession(langAlias: String, src: String, input: String?) =
        sessionRepository.addQueue(langAlias, src.encodeToByteArray(), input?.encodeToByteArray())?.toRespondSession()
}