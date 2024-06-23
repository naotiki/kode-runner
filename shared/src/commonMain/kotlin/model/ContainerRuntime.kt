package model

import java.io.File

data class ContainerRuntime(
    val id:String,
    val runtimeData: RuntimeData,
    val dockerfile: File
)
