package com.sourceforgery.tachikoma.buildsrc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import sourceSets

class TachikomaJavaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.javaSetup()
    }
}

private fun Project.javaSetup() {
    apply(plugin = "java-library")

    val assemble by tasks.getting

    val javadocJar by tasks.registering(Jar::class) {
        val javadoc by tasks.getting(Javadoc::class)
        dependsOn(javadoc)
        from(javadoc.destinationDir)
        archiveClassifier.set("javadoc")
    }

    tasks.withType(Javadoc::class.java) {
        val opts = options as StandardJavadocDocletOptions
        opts.addStringOption("Xdoclint:none", "-quiet")
    }

    val sourceJar by tasks.registering(Jar::class) {
        from(sourceSets["main"].allJava)
        archiveClassifier.set("source")
    }

    tasks.withType(JavaCompile::class.java).configureEach {
        options.compilerArgs = listOf("-Xlint:unchecked", "-Xlint:deprecation")
    }

    assemble.dependsOn(sourceJar)
    assemble.dependsOn(javadocJar)
}
