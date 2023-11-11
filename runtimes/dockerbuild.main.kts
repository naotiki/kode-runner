@file:DependsOn("com.github.docker-java:docker-java:3.3.4")
@file:DependsOn("com.github.docker-java:docker-java-transport-httpclient5:3.3.4")

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import java.io.FileNotFoundException
import java.time.Duration

private fun getInstanceDockerClient(): DockerClient {
    val standardDockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().apply {

    }.build()
    val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(standardDockerClientConfig.dockerHost)
        .sslConfig(standardDockerClientConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()
    return DockerClientImpl.getInstance(standardDockerClientConfig, dockerHttpClient)
}

getInstanceDockerClient()
