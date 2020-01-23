package com.sourceforgery.tachikoma.buildsrc

import se.transmode.gradle.plugins.docker.DockerTask
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import com.sourceforgery.tachikoma.buildsrc.CheckDuplicateClassesTask
import grpcVersion
import hk2Version
import implementation
import java.io.File
import java.math.BigInteger
import java.net.URL
import kotlinVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.existing
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import protocVersion
import sourceSets
import spekVersion
import testImplementation

fun Project.dockerSetup() {
    apply(plugin = "base")
    apply(plugin = "docker")

    group = "sourceforgery"

    tasks.register("downloadTini") {
        val tiniUrl = URL("https://github.com/krallin/tini/releases/download/v0.16.1/tini-static-amd64")
        val tiniChecksum = "d1cb5d71adc01d47e302ea439d70c79bd0864288"
        val tiniBinary = java.io.File(project.buildDir, "tini")
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