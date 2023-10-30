package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Image
import com.github.dockerjava.api.model.Statistics
import repository.DockerRepository

class DockerRepositoryImpl(private val dockerApi:DockerClient) :DockerRepository {
    override fun ping(){
        dockerApi.pingCmd().exec()
    }

    override fun listImages(): List<Image> {
        return dockerApi.listImagesCmd().exec()
    }

    override fun launch() {
        val a=dockerApi.createContainerCmd("").withHostConfig(HostConfig.newHostConfig())

    }
}
