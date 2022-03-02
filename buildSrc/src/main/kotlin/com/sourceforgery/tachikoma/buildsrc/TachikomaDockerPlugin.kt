package com.sourceforgery.tachikoma.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import recurseTasks
import se.transmode.gradle.plugins.docker.DockerTask
import java.io.File
import java.net.URL

class TachikomaDockerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.dockerSetup()
    }
}

private fun Project.dockerSetup() {
    apply(plugin = "base")
    apply(plugin = "docker")

    group = "sourceforgery"

    tasks.register("downloadTini") {
        val tiniUrl = URL("https://github.com/krallin/tini/releases/download/v0.16.1/tini-static-amd64")
        val tiniChecksum = "d1cb5d71adc01d47e302ea439d70c79bd0864288"
        val tiniBinary = File(project.buildDir, "tini")
        inputs.property("tiniUrl", tiniUrl)
        inputs.property("tiniChecksum", tiniChecksum)
        outputs.file(tiniBinary)
        doLast {
            project.buildDir.mkdirs()
            if (!tiniBinary.exists()) {
                val bytes = tiniUrl.openStream().use { it.readBytes() }
                tiniBinary.outputStream().buffered().use {
                    it.write(bytes)
                }
            }

            val digest = java.security.MessageDigest.getInstance("SHA1").digest(tiniBinary.readBytes())
            val calculatedChecksum = java.math.BigInteger(1, digest).toString(16)
            if (calculatedChecksum != tiniChecksum) {
                throw RuntimeException("Failed to download valid tini. Please remove $tiniBinary and try again $tiniChecksum != $calculatedChecksum")
            }
        }
    }

    val dockerTask by tasks.registering { }

    val pushDockerSnapshot by tasks.registering {
        dependsOn(dockerTask)
        val snapshotDockerRepo: String? by project
        outputs.upToDateWhen {
            // Never up to date since we don't know the hash of the dockerTask task
            // without running lots of commands that are as slow as running the actual task
            false
        }
        val repo by lazy {
            requireNotNull(snapshotDockerRepo) { "Need to have snapshotDockerRepo set to a repo (e.g. -DsnapshotDockerRepo=gcr.io/tachikoma-staging/ that will be used in the deployment yaml" }
                .trimEnd('/')
        }
        val snapshotDockerVersion: String? by project

        // Make sure that all images are tagged
        val dockerTasks = dockerTask.get()
            .recurseTasks()
            .filterIsInstance<DockerTask>()
            .toList()
        for (aDockerTask in dockerTasks) {
            // Must be here for.. reasons?
            val originalImageTag = aDockerTask.imageTag
            doLast {
                val snapshotTag = "$repo/${aDockerTask.applicationName}:${snapshotDockerVersion ?: version}"
                exec {
                    commandLine("/usr/bin/env", "docker", "tag", originalImageTag, snapshotTag)
                }
                exec {
                    commandLine("/usr/bin/env", "docker", "push", snapshotTag)
                }
            }
        }
    }

    rootProject.tasks.named("publishSnapshot") {
        dependsOn(pushDockerSnapshot)
    }

    tasks["build"].dependsOn(dockerTask)
    rootProject.tasks["publish"].dependsOn(dockerTask)
}
