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
    implementation(libs.kotlinx.coroutines)
    // https://mvnrepository.com/artifact/com.github.docker-java/docker-java
    implementation(libs.docker.api)
    implementation(libs.docker.api.transport.apache)
    implementation(libs.koin.core)

    testImplementation(libs.koin.test)
    testImplementation(kotlin("test"))
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
