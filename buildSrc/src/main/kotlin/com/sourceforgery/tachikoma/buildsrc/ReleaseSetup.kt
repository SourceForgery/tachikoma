package com.sourceforgery.tachikoma.buildsrc

import co.riiid.gradle.GithubExtension
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.getByType
import java.io.File

fun Project.releaseSetup() {
    apply(plugin = "net.researchgate.release")
    apply(plugin = "co.riiid.gradle")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    val circleTag = System.getenv("CIRCLE_TAG") ?: ""

    val publishTask = tasks.getByName("publish")

    if (circleTag.isNotEmpty()) {
        // Only activate when we"re building a tag (release)
        if (System.getenv("GITHUB_API_TOKEN") == null) {
            error("GITHUB_API_TOKEN not set")
        }
        println("Trying to build release")
        publishTask.finalizedBy("githubRelease")

    }

    extensions.getByType<GithubExtension>().apply {
        owner = "SourceForgery"
        repo = "tachikoma"
        token = System.getenv("GITHUB_API_TOKEN") ?: "xx"
        tagName = circleTag
        targetCommitish = "master"
        name = "v${project.version}"
    }


    val currentBranch = System.getenv("CIRCLE_BRANCH")
        ?: let {
            FileRepository(File(project.rootDir, ".git")).use {
                it.branch
            }
        }
    val dockerPushRelease = when {
        currentBranch == "master" && System.getenv("DOCKER_PUSH")?.toBoolean() == true -> true
        circleTag.isNotEmpty() -> true
        else -> false
    }

    rootProject.extensions.extraProperties["dockerPush"] = dockerPushRelease
}