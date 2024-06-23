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
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentnegotiation)

    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.serialization.cbor)

    implementation(libs.kotlinx.rpc.runtime.client)
    implementation(libs.kotlinx.rpc.runtime.serialization.cbor)
    implementation(libs.kotlinx.rpc.transport.ktor.client)

    implementation(libs.slf4j)
    implementation(libs.kord)

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
