plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.rpc)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotest.multiplatform)
}

group = "me.naotiki"
version = "unspecified"


kotlin {
    jvmToolchain(8)
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines)

            implementation(libs.kotlinx.rpc.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.framework.datatest)
            implementation(libs.kotest.property)
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

tasks.named<Test>("jvmTest"){
    useJUnitPlatform()
}