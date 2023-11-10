package repository

import model.ContainerRuntime
import model.RuntimeData

interface RuntimeRepository {
    fun listRuntimes(): List<RuntimeData>
    fun searchRuntimeData(alias: String): RuntimeData?
    fun searchContainerRuntime(alias: String): ContainerRuntime?
    fun regenerateRuntimeList()
    fun listContainerRuntimes(): List<ContainerRuntime>
}
