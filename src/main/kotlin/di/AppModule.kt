package di

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.koin.dsl.module
import repository.DockerRepository
import repository.impl.DockerRepositoryImpl
import java.time.Duration


val appModule= module {
    single {
        getInstanceDockerClient()
    }
    single<DockerRepository> { DockerRepositoryImpl(get()) }
}

private fun getInstanceDockerClient(): DockerClient {
    val standardDockerClientConfig= DefaultDockerClientConfig.createDefaultConfigBuilder().apply {

    }.build()
    val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(standardDockerClientConfig.dockerHost)
        .sslConfig(standardDockerClientConfig.sslConfig)
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build()
    return DockerClientImpl.getInstance(standardDockerClientConfig,dockerHttpClient)
}
