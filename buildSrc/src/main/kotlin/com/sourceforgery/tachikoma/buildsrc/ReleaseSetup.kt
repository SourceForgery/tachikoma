package com.sourceforgery.tachikoma.buildsrc

import co.riiid.gradle.GithubExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayExtension.PackageConfig
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.getByType
import java.io.File
import addAssets

fun Project.releaseSetup() {
    apply(plugin = "net.researchgate.release")
    apply(plugin = "co.riiid.gradle")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.jfrog.bintray")

    val travisTag = System.getenv("TRAVIS_TAG") ?: ""

    val publishTask = tasks.getByName("publish")

    if (travisTag.isNotEmpty()) {
        // Only activate when we"re building a tag (release)
        if (System.getenv("GITHUB_API_TOKEN") == null) {
            error("GITHUB_API_TOKEN not set")
        }
        println("Trying to build release")
        publishTask.finalizedBy("githubRelease")
    }

    afterEvaluate {
        extensions.configure<GithubExtension>("github") {
            owner = "SourceForgery"
            repo = "tachikoma"
            token = System.getenv("GITHUB_API_TOKEN") ?: "xx"
            tagName = travisTag
            targetCommitish = "master"
            name = "v${project.version}"
            addAssets(listOf("${project.buildDir}/kubernetes/deployment-webserver.yaml"))
        }
    }


    val currentBranch = System.getenv("TRAVIS_BRANCH")
        ?: let {
            FileRepository(File(project.rootDir, ".git")).use {
                it.branch
            }
        }
    val dockerPushRelease = when {
        currentBranch == "master" && System.getenv("DOCKER_PUSH")?.toBoolean() == true -> true
        travisTag.isNotEmpty() -> true
        else -> false
    }

    rootProject.extensions.extraProperties["dockerPush"] = dockerPushRelease

    extensions.getByType<BintrayExtension>().apply {
        publish = true
        setPublications()
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        pkg(closureOf<PackageConfig> {
            repo = "Tachikoma"
            name = "tachikoma"
        })
    }


    subprojects {
        if (travisTag.isNotEmpty()) {
            publishTask.finalizedBy("bintrayUpload")
        }
    }
}