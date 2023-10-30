import com.github.dockerjava.api.model.Image
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.DockerRepository

class CodeRunnerApplication:KoinComponent {
    private val dockerRepo:DockerRepository by inject()
    fun ping(){
        dockerRepo.launch()
    }
    fun listImages(): List<Image> {
        return dockerRepo.listImages()
    }
}
