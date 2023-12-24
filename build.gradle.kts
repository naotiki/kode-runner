plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    application
}

group = "me.naotiki"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.core)
    implementation(libs.ktor.netty)
    implementation(libs.ktor.network)
    implementation(libs.ktor.websockets)
    implementation(libs.ktor.contentnegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.serialization.cbor)
    implementation(libs.ktor.swagger)
   // implementation(libs.slf4j)
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.hocon)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.coroutines)
    // https://mvnrepository.com/artifact/com.github.docker-java/docker-java
    implementation(libs.docker.api)
    implementation(libs.docker.api.transport.apache)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.yamlkt)
    implementation(libs.nanoid)

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
