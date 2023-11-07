package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.*
import repository.DockerRepository
import java.io.File
private const val SESSION_PATH="/work/session/"
class DockerRepositoryImpl(private val dockerApi: DockerClient) : DockerRepository {
    private val hostConfig: HostConfig = HostConfig.newHostConfig().apply {
        withMemory(1024*1024*1024)
        withNanoCPUs(100000000)
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

    // 自動でcleanupされる
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
