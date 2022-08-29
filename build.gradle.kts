import com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Clock

plugins {
    id("com.github.ben-manes.versions")
    id("net.researchgate.release") version "2.8.1"
    id("com.github.breadmoirai.github-release")
    `java-library`
}

val replaceVersion by tasks.registering(Copy::class) {
    // Always regenerate yaml
    outputs.upToDateWhen { false }
    from("kubernetes") {
        include("**/*.yaml")
        val snapshotDockerRepo: String? by project
        val snapshotDockerVersion: String? by project

        val replacements = mutableMapOf(
            "version" to (snapshotDockerVersion ?: project.version),
            "dockerRepository" to (snapshotDockerRepo?.trimEnd('/') ?: "sourceforgery"),
            "currentTime" to Clock.systemUTC().instant().toString()
        )
        expand(replacements)
    }
    into("$buildDir/kubernetes/")
    includeEmptyDirs = false
}

tasks.assemble {
    dependsOn(replaceVersion)
}

val publishSnapshot by tasks.registering {
    dependsOn(replaceVersion)
}
tasks.githubRelease {
    dependsOn(replaceVersion)
}

rootProject.extensions.configure<GithubReleaseExtension> {
    releaseAssets.from("$buildDir/kubernetes/deployment-webserver.yaml")
}

val publish by tasks.registering {
    dependsOn(tasks.assemble)
}

@Suppress("UnstableApiUsage")
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
            dependencySubstitution {
                substitute(module("org.slf4j:jcl-over-slf4j")).using(module("org.apache.logging.log4j:log4j-jcl:$log4j2Version"))

                substitute(module("org.slf4j:jul-to-slf4j")).using(module("org.apache.logging.log4j:log4j-jul:$log4j2Version"))
                substitute(module("org.slf4j:slf4j-simple")).using(module("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version"))
                substitute(module("com.google.guava:guava-jdk5")).using(module("com.google.guava:guava:$guavaVersion"))

                all {
                    when (val requested = requested) {
                        is org.gradle.internal.component.external.model.DefaultModuleComponentSelector ->
                            when (requested.group) {
                                "io.grpc" -> if ("kotlin" !in requested.module) {
                                    useTarget("${requested.group}:${requested.module}:$grpcVersion")
                                }

                                "com.google.protobuf" -> useTarget("${requested.group}:${requested.module}:$protocVersion")
                                "org.apache.logging.log4j" -> if (requested.module != "log4j-api-kotlin") {
                                    useTarget("${requested.group}:${requested.module}:$log4j2Version")
                                }

                                "org.jetbrains.kotlin" -> useTarget("${requested.group}:${requested.module}:$kotlinVersion")
                                "com.fasterxml.jackson.core" -> useTarget("${requested.group}:${requested.module}:$jacksonVersion")
                            }
                    }
                }
            }
            force(
                "com.google.errorprone:error_prone_annotations:2.9.0",
                "net.bytebuddy:byte-buddy:$bytebuddyVersion",
                "com.google.guava:guava:$guavaVersion",
                "commons-io:commons-io:2.6",
                "commons-logging:commons-logging:1.2",
                "javax.annotation:javax.annotation-api:1.3.2",
                "org.postgresql:postgresql:$postgresqlDriverVersion",
                "com.google.code.gson:gson:2.8.9",
                "org.slf4j:slf4j-api:1.7.29",
            )
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.apply {
            isWarnings = true
        }
    }

    tasks.withType<GenerateModuleMetadata> {
        suppressedValidationErrors.add("enforced-platform")
    }
}

group = "com.sourceforgery.tachikoma"

val currentTag = System.getenv("CIRCLE_TAG") ?: ""

if (currentTag.isNotEmpty()) {
    fun gitHistorySinceLastTag(): String =
        ByteArrayOutputStream().use { baos ->
            exec {
                commandLine = listOf("git", "describe", "--tags", "--abbrev=0", "@^")
                standardOutput = baos
            }
            val previousTag = baos.toString(StandardCharsets.UTF_8).trim()
            baos.reset()
            exec {
                commandLine = listOf("git", "log", "--oneline", "$previousTag..@")
                standardOutput = baos
            }
            baos.toString(StandardCharsets.UTF_8)
        }

    // Only activate when we're building a tag (release)
    println("Trying to build release")
    tasks["publish"].finalizedBy("githubRelease")
    githubRelease {
        token(requireNotNull(System.getenv("GITHUB_API_TOKEN")) { "GITHUB_API_TOKEN not set" })
        owner.set("SourceForgery")
        repo.set("tachikoma")
        tagName.set(currentTag)
        targetCommitish.set("master")
        releaseName.set("v${project.version}")
        releaseAssets
        body {
            gitHistorySinceLastTag()
        }
    }
}

val currentBranch = System.getenv("CIRCLE_BRANCH")
    ?: let {
        org.eclipse.jgit.internal.storage.file.FileRepository(File(project.rootDir, ".git")).use {
            it.branch
        }
    }
val dockerPushRelease = when {
    currentBranch == "master" && System.getenv("DOCKER_PUSH")?.toBoolean() == true -> true
    currentTag.isNotEmpty() -> true
    else -> false
}

rootProject.extensions.extraProperties["dockerPush"] = dockerPushRelease
