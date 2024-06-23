package model

import model.ContainerRuntime
import java.io.File

data class SessionData(
    val sessionId: String,
    val sessionDir:File,
    val sourceFile: File,
    val containerRuntime: ContainerRuntime,
    val inputFile:File?=null,
    val containerId:String?=null
)
