package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.*
import data.DataSize
import repository.ConfigurationRepository
import repository.DockerRepository
import java.io.File

private const val SESSION_PATH = "/home/runner/work"

class DockerRepositoryImpl(private val dockerApi: DockerClient, private val configRepo: ConfigurationRepository) :
    DockerRepository {
    private val dockerConfig get() = configRepo.get().docker
    private fun getHostConfig(): HostConfig = HostConfig.newHostConfig().apply {
        val (runtime) = configRepo.get()
        withNetworkMode("none")
        withMemory(runtime.memory.bytes)
        runtime.diskQuota?.let {
            withStorageOpt(mapOf("size" to it.toString()))
        }
        withNanoCPUs(runtime.nanoCpu)
        withPidsLimit(runtime.pids)
        runtime.runtime?.let {
            withRuntime(it)
        }
    }

    override fun ping() {
        dockerApi.pingCmd().exec()
    }

    override fun listImages(): List<Image> {
        return dockerApi.listImagesCmd().exec()
    }

    override fun cleanup(containerId: String) {
        val shouldForce = kotlin.runCatching { dockerApi.stopContainerCmd(containerId).exec() }.isFailure
        kotlin.runCatching { dockerApi.removeContainerCmd(containerId).withForce(shouldForce).exec() }.onFailure {
            dockerApi.removeContainerCmd(containerId).withForce(true).exec()
        }
    }

    // 失敗で自動でcleanupされる
    override fun prepareContainer(imageId: String, copySourceDir: File): String {
        val containerId =
            dockerApi.createContainerCmd(dockerConfig.imagePrefix + imageId +":${dockerConfig.imageTag}").withHostConfig(getHostConfig()).withTty(true)
                .exec().id
        try {
            dockerApi.copyArchiveToContainerCmd(containerId).withHostResource(copySourceDir.absolutePath)
                .withRemotePath(SESSION_PATH).withDirChildrenOnly(true).exec()
            dockerApi.startContainerCmd(containerId).exec()
        } catch (e: Exception) {
            cleanup(containerId)
            throw e
        }
        return containerId
    }

    override fun inspectExitCodeExec(execId: String): Long {
        return dockerApi.inspectExecCmd(execId).exec().exitCodeLong
    }

    override fun execCmdContainer(
        containerId: String,
        command: Array<String>,
        adapter: Adapter<Frame>,
        inputFile: File?
    ): Pair<String, Adapter<Frame>> {
        val r = dockerApi.execCreateCmd(containerId)
            .withCmd(*command)
            .withAttachStdout(true)
            .withAttachStdin(inputFile != null)
            .withAttachStderr(true).exec()
        return r.id to dockerApi.execStartCmd(r.id)
            .withStdIn(inputFile?.inputStream())
            .exec(adapter)
    }

    override fun buildImage(dockerFile: File, imageTag: String): Adapter<BuildResponseItem> {
        println("start: rebuild $imageTag")
        return dockerApi.buildImageCmd(dockerFile)
            .withRemove(true)
            .withTags(setOf(dockerConfig.imagePrefix + imageTag +":${dockerConfig.imageTag}"))
            .exec(object : Adapter<BuildResponseItem>() {
                override fun onComplete() {
                    super.onComplete()
                    println("done: rebuild $imageTag")
                }
            })
    }

    override fun pullImage(imageName: String): Adapter<PullResponseItem> {
        println("start: pull $imageName")
        return dockerApi.pullImageCmd(dockerConfig.imagePrefix + imageName +":${dockerConfig.imageTag}").exec(object : Adapter<PullResponseItem>() {
            override fun onComplete() {
                super.onComplete()
                println("done: pull $imageName")
            }
        })
    }
}
