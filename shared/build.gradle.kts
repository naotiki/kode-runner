plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.rpc)
    alias(libs.plugins.kotlinPluginSerialization)

}

group = "me.naotiki"
version = "unspecified"



/*tasks.test {
    useJUnitPlatform()
}*/
kotlin {
    jvmToolchain(17)
    jvm()
    sourceSets{
        commonMain.dependencies {
            api(libs.kotlinx.coroutines)

            implementation(libs.kotlinx.rpc.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}