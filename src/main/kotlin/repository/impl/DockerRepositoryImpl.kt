package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.*
import repository.DockerRepository
import java.io.File
private const val DOCKER_IMAGE_PREFIX = "code-runner/"
private const val SESSION_PATH="/work/session/"
private const val MEMORY_MAX_B:Long =  1024/*MB*/*1024*1024
private const val NANOCPU:Long = 1000000000//1 CPU
class DockerRepositoryImpl(private val dockerApi: DockerClient) : DockerRepository {
    private val hostConfig: HostConfig = HostConfig.newHostConfig().apply {
        withMemory(MEMORY_MAX_B)

        withNanoCPUs(NANOCPU)
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
        val containerId = dockerApi.createContainerCmd(DOCKER_IMAGE_PREFIX+imageId).withHostConfig(hostConfig).withTty(true).exec().id
        try {
            dockerApi.copyArchiveToContainerCmd(containerId).withHostResource(copySourceDir.absolutePath).withRemotePath(SESSION_PATH).withDirChildrenOnly(true).exec()
            dockerApi.startContainerCmd(containerId).exec()
        } catch (e:Exception) {
            cleanup(containerId)
            throw e
        }
        return containerId
    }

    override fun inspectExitCodeExec(execId: String): Long {
        return dockerApi.inspectExecCmd(execId).exec().exitCodeLong
    }
    override fun execCmdContainer(containerId: String, command:Array<String>, adapter: Adapter<Frame>): Pair<String,Adapter<Frame>> {
        val r = dockerApi.execCreateCmd(containerId).withCmd(*command).withAttachStdout(true).withAttachStderr(true).exec()
        return r.id to dockerApi.execStartCmd(r.id).exec(adapter)
    }

    override fun buildImage(dockerFile: File, imageTag: String): Adapter<BuildResponseItem> {
        println("start: rebuild $imageTag")
        return dockerApi.buildImageCmd(dockerFile)
            .withRemove(true)
            .withTags(setOf(DOCKER_IMAGE_PREFIX+imageTag))
            .exec(object : Adapter<BuildResponseItem>(){
                override fun onComplete() {
                    super.onComplete()
                    println("done: rebuild $imageTag")
                }
            })
    }
}
