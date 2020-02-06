package com.sourceforgery.tachikoma.buildsrc

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import java.io.File
import java.net.URL

fun Project.dockerSetup() {
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

    val dockerTask by tasks.registering() {}

    tasks["assemble"].dependsOn(dockerTask)
}