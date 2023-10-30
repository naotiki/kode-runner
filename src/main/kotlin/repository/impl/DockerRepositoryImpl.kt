package repository.impl

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Image
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
