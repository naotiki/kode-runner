package repository.impl

import model.ContainerRuntime
import model.RuntimeData
import net.mamoe.yamlkt.Yaml
import repository.RuntimeRepository
import java.io.File
import java.io.FileNotFoundException

const val DOCKERFILE_NAME = "Dockerfile"
const val MANIFEST_NAME = "manifest.yml"




class RuntimeRepositoryImpl(private val directory: File) : RuntimeRepository {

    private var containerRuntimeMap: List<Pair<List<String>, ContainerRuntime>>
    private fun generateRuntimeList(): List<Pair<List<String>, ContainerRuntime>> {
        val dir = directory.listFiles { it, name -> it.resolve(name).isDirectory } ?: throw FileNotFoundException()
        return dir.map {
            val manifest = it.resolve(MANIFEST_NAME)
            val dockerFile = it.resolve(DOCKERFILE_NAME)
            if (!manifest.canRead() || !dockerFile.canRead()) throw FileNotFoundException("at ${it.absolutePath}")
            val runtimeData = Yaml.decodeFromString(RuntimeData.serializer(), manifest.readText())
            runtimeData.alias to ContainerRuntime(runtimeData.id,runtimeData, dockerFile)
        }
    }

    override fun regenerateRuntimeList(){
        containerRuntimeMap = generateRuntimeList()
    }

    init {
        directory.mkdir()
        containerRuntimeMap = generateRuntimeList()
    }

    override fun listContainerRuntimes():List<ContainerRuntime>{
        return containerRuntimeMap.map { it.second }
    }

    override fun listRuntimes(): List<RuntimeData> {
        return containerRuntimeMap.map { it.second.runtimeData }
    }

    override fun searchContainerRuntime(alias: String): ContainerRuntime? {
        return containerRuntimeMap.singleOrNull { (k, _)->
            alias in k
        }?.second
    }


    override fun searchRuntimeData(alias: String): RuntimeData? {
        return searchContainerRuntime(alias)?.runtimeData
    }

}
