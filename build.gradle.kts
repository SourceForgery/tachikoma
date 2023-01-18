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

val javaVersion: String by project

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
