import org.gradle.kotlin.dsl.support.zipTo

/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.kotlinx.rpc) apply false
    alias(libs.plugins.kotlinx.rpc.platform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotest.multiplatform) apply false
    alias(libs.plugins.ktor) apply false

    base
}

fun Task.dependsEach(path: String, vararg projects: Project) {
    dependsOn(*(projects.map { ":${it.name}:$path" }.toTypedArray()))
}

tasks.create("allBuild") {
    group = "build"
    val targetProjects = listOf(projects.server, projects.discordBot).map {
        project(it.name)
    }.toTypedArray()
    dependsEach("installDist", *targetProjects)
    val dest = buildDir.resolve("install")
    val zip=buildDir.resolve("app.zip")
    outputs.dir(dest)
    outputs.file(zip)
    doLast {
        copy {
            from(targetProjects.map {
                it.buildDir.resolve("install")
            })
            into(dest)
            duplicatesStrategy=DuplicatesStrategy.INCLUDE
        }

        zipTo(zip, dest)
    }
}