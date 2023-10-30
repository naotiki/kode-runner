import di.appModule
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import repository.DockerRepository

fun main(args: Array<String>) {
    startKoin { modules(appModule) }

    println("Hello World!")
    println(CodeRunnerApplication().listImages())
    CodeRunnerApplication().ping()
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}
