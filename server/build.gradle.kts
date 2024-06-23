plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinx.rpc.platform)
    application
}

group = "me.naotiki"
version = "1.0-SNAPSHOT"


dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.contentnegotiation)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.serialization.cbor)
    implementation(libs.ktor.network)
   // implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.hocon)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.kotlinx.coroutines)
    // https://mvnrepository.com/artifact/com.github.docker-java/docker-java
    implementation(libs.docker.api)
    implementation(libs.docker.api.transport.apache)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.yamlkt)
    implementation(libs.nanoid)

    implementation(libs.kotlinx.rpc.runtime.server)
    implementation(libs.kotlinx.rpc.runtime.serialization.cbor)
    implementation(libs.kotlinx.rpc.transport.ktor.server)

  //  testImplementation(libs.koin.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.runner)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}

distributions{
    main{
        contents {
            from("runtimes/"){
                into("runtimes/")
            }
        }
    }
}
