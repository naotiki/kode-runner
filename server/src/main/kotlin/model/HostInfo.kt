package model

import kotlinx.serialization.Serializable

@Serializable
data class RunnerHostInfo(
    val name:String,
    val config: Configuration
)
