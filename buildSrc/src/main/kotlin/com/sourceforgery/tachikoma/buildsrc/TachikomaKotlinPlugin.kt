package com.sourceforgery.tachikoma.buildsrc

import implementation
import kotlinVersion
import kotlinCoroutineVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import sourceSets
import testImplementation

@Suppress("UnstableApiUsage")
class TachikomaKotlinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.kotlinSetup()
    }
}

private fun Project.kotlinSetup() {
    apply(plugin = "kotlin")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    dependencies {
        implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutineVersion"))
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")

        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        testImplementation("org.junit.platform:junit-platform-runner:1.8.2")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            languageVersion = "1.6"
            apiVersion = "1.6"
            jvmTarget = "11"
            freeCompilerArgs = listOf(
                "-java-parameters",
                "-Xjsr305=strict",
                "-Xjvm-default=enable",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }

    extensions.getByType<KtlintExtension>().apply {
        debug.set(false)
        verbose.set(true)
        android.set(false)
        ignoreFailures.set(false)
        filter {
            exclude { "/generated/" in it.file.path }
        }
        disabledRules.set(listOf("final-newline"))
        version.set("0.44.0")
    }

    val sourceJar by tasks.registering(Jar::class) {
        from(sourceSets["main"].allJava)
        archiveClassifier.set("source")
    }
    val assemble by tasks
    assemble.dependsOn(sourceJar)

    val checkDuplicateClasses by tasks.registering(CheckDuplicateClassesTask::class)

    tasks["check"].dependsOn(checkDuplicateClasses)
}