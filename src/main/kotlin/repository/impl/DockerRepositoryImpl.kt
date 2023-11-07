package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.*
import repository.DockerRepository
import java.io.File
private const val SESSION_PATH="/work/session/"
private const val MEMORY_MAX_B:Long =  500/*MB*/*1024*1024
private const val NANOCPU:Long = 100000000//0.1 CPU
class DockerRepositoryImpl(private val dockerApi: DockerClient) : DockerRepository {
    private val hostConfig: HostConfig = HostConfig.newHostConfig().apply {
        withMemory(MEMORY_MAX_B)
        withNanoCPUs(NANOCPU)
        withStorageOpt(mutableMapOf("size" to "2G"))
    }

    override fun ping() {
        dockerApi.pingCmd().exec()
    }

    override fun listImages(): List<Image> {
        return dockerApi.listImagesCmd().exec()
    }

    override fun cleanup(containerId:String){
        dockerApi.stopContainerCmd(containerId).exec()
        dockerApi.removeContainerCmd(containerId).exec()
    }

    // 失敗で自動でcleanupされる
    override fun prepareContainer(imageId:String, copySourceDir:File):String{
        val containerId = dockerApi.createContainerCmd(imageId).withHostConfig(hostConfig).withTty(true).exec().id
        try {
            dockerApi.copyArchiveToContainerCmd(containerId).withHostResource(copySourceDir.absolutePath).withRemotePath(SESSION_PATH).withDirChildrenOnly(true).exec()
            dockerApi.startContainerCmd(containerId).exec()
        } catch (e:Exception) {
            cleanup(containerId)
            throw e
        }
        return containerId
    }
    override fun execCmdContainer(containerId: String, command:Array<String>, adapter: Adapter<Frame>): Adapter<Frame> {
        val r = dockerApi.execCreateCmd(containerId).withCmd(*command).withAttachStdout(true).withAttachStderr(true).exec()
        return dockerApi.execStartCmd(r.id).exec(adapter)
    }
}
