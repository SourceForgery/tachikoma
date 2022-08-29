package com.sourceforgery.tachikoma.buildsrc.docker

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import se.transmode.gradle.plugins.docker.DockerTask

abstract class DockerTagImage : DefaultTask() {
    @get:Input
    abstract val sourceTag: Property<DockerTag>

    @get:Input
    abstract val targetTag: Property<DockerTag>

    @get:Input
    abstract val version: Property<String>

    init {
        @Suppress("LeakingThis")
        version.convention(
            project.provider {
                project.version.toString()
            }
        )
    }

    @TaskAction
    fun tagImage() {
        project.exec {
            commandLine = listOf(
                "docker",
                "tag",
                sourceTag.get().fullTag,
                targetTag.get().fullTag
            )
        }
    }
}

abstract class DockerPush : DefaultTask() {
    @get:Input
    abstract val tag: Property<DockerTag>

    @get:Input
    abstract val version: Property<String>

    init {
        @Suppress("LeakingThis")
        version.convention(
            project.provider {
                project.version.toString()
            }
        )
    }

    @TaskAction
    fun pushTag() {
        project.exec {
            commandLine = listOf(
                "docker",
                "push",
                tag.get().fullTag
            )
        }
    }
}

fun DockerTask.aptInstall(vararg packages: String) = aptInstall(packages.toSet())

fun DockerTask.aptInstall(packages: Iterable<String>) {
    val packagesToInstall = packages.joinToString(" ")
    runMultilineCommand(
        """
            apt-get update
            apt-get -y --no-install-recommends install $packagesToInstall
            apt-get clean
            rm -rf /var/lib/apt/lists/*
        """
    )
}

fun DockerTask.runMultilineCommand(multilineCommand: String) = runCommand(multilineCommand.trimIndent().replace(Regex("\\s*\n\\s*"), " && "))

fun DockerTask.installCloudSql() {
    runMultilineCommand(
        """
            curl -Ss https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -o /usr/bin/cloud_sql_proxy
            chmod 0755 /usr/bin/cloud_sql_proxy
        """
    )
}

private fun Boolean.ifTrue(ifTrue: String): String =
    if (this) {
        ifTrue
    } else {
        ""
    }

@Suppress("UNCHECKED_CAST")
fun Project.replaceDockerTask(config: DockerTask.() -> Unit) {
    afterEvaluate {
        afterEvaluate {
            tasks.named("webserverDocker", DockerTask::class.java) {
                instructions.clear()
                stageBacklog.clear()
                config()
            }
        }
    }
}

val DockerTask.combinedTag: Provider<DockerTag>
    get() {
        return project.provider {
            DockerTag("${imageTag.get()}")
        }
    }
