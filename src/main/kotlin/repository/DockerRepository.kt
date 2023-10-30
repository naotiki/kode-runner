package repository

import com.github.dockerjava.api.model.Image

interface DockerRepository {
    fun ping()
    fun listImages():List<Image>

    fun launch()
}
