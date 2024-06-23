package service

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RPC
import model.RespondSession
import model.RunnerEvent
import model.SessionData

interface RunnerService:RPC {
    suspend fun executeSession(sessionId:String): Flow<RunnerEvent>
    suspend fun createSession(langAlias:String,src:String,input:String?): RespondSession?
}