package repository

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.Image
import java.io.File

interface DockerRepository {
    fun ping()
    fun listImages():List<Image>

    fun cleanup(containerId: String)
    fun prepareContainer(imageId: String, copySourceDir: File): String

    fun buildImage(dockerFile: File,imageTag:String):Adapter<BuildResponseItem>
    fun inspectExitCodeExec(execId:String):Long
    fun execCmdContainer(
        containerId: String,
        command: Array<String>,
        adapter: Adapter<Frame>,
        inputFile: File?
    ): Pair<String, Adapter<Frame>>
}
