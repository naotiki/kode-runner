package repository.impl

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import model.Configuration
import repository.ConfigurationRepository
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class ConfigurationRepositoryImpl : ConfigurationRepository {
    private var configuration: Configuration

    init {
        File(CONFIG_FILE_PATH).takeUnless { it.exists() }?.run {
            createNewFile()
            writeText(
                ConfigHocon.encodeToConfig(Configuration.serializer(), getDefault()).renderRoot()
            )
        }
        configuration = load()
    }

    private fun getDefault(): Configuration {
        return Configuration()
    }

    override fun load(): Configuration {
        val conf = ConfigFactory.parseFile(File(CONFIG_FILE_PATH))
        return ConfigHocon.decodeFromConfig(Configuration.serializer(), conf)
    }


    override fun get() = configuration

    override fun reload() {
        configuration = load()
    }

    companion object {
        private const val CONFIG_FILE_PATH = "application.conf"

        @OptIn(ExperimentalSerializationApi::class)
        private val ConfigHocon = Hocon {
            encodeDefaults = true
        }

        private fun Config.renderRoot(): String {
            return root().render(
                ConfigRenderOptions.defaults().setJson(false).setOriginComments(false)
            )
        }
    }
}
