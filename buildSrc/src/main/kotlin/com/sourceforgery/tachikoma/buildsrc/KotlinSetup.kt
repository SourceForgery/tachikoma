package com.sourceforgery.tachikoma.buildsrc

import com.sourceforgery.tachikoma.buildsrc.CheckDuplicateClassesTask
import hk2Version
import implementation
import kotlinVersion
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import sourceSets
import spekVersion
import testImplementation

fun Project.kotlinSetup() {
    apply(plugin = "kotlin")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.dokka")

    dependencies {
        testImplementation("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        testImplementation("org.junit.platform:junit-platform-runner:1.1.0")
        testImplementation("org.glassfish.hk2:hk2-locator:$hk2Version")
        testImplementation("org.glassfish.hk2:hk2-utils:$hk2Version")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            languageVersion = "1.2"
            apiVersion = "1.2"
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable")
        }
    }

    extensions.getByType<KtlintExtension>().apply {
        debug.set(false)
        verbose.set(true)
        android.set(false)
//            reporter = "checkstyle"
        ignoreFailures.set(false)
        disabledRules.set(listOf("final-newline"))
    }

    val dokka by tasks.getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
    }

    val sourceJar by tasks.registering(Jar::class) {
        from(sourceSets.getByName("main").allJava)
        archiveClassifier.set("source")
    }
    val assemble by tasks
    assemble.dependsOn(sourceJar)

    val dokkaJar by tasks.registering(Jar::class) {
        dependsOn(dokka)
        from(dokka.outputDirectory)
        archiveClassifier.set("javadoc")
    }
    assemble.dependsOn(dokkaJar)

    val checkDuplicateClasses by tasks.registering(CheckDuplicateClassesTask::class)

    tasks["check"].dependsOn(checkDuplicateClasses)
}