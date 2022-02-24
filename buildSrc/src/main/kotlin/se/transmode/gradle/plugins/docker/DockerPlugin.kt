/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.transmode.gradle.plugins.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.get

internal class DockerPlugin : Plugin<Project> {
    lateinit var extension: DockerPluginExtension

    override fun apply(project: Project) {
        this.extension = createExtension(project)

        // FIXME: Application plugin must be applied before docker plugin for this to work.
        // FIXME: Check for application AND java plugin
        project.plugins.withType(ApplicationPlugin::class.java).configureEach {
            project.addDistDockerTask()
        }
    }

    private fun Project.addDistDockerTask() {
        tasks.register("distDocker", DockerTask::class.java) {
            val distTar = tasks[ApplicationPlugin.TASK_DIST_TAR_NAME] as AbstractArchiveTask
            dependsOn(distTar)
            group = "docker"
            description = "Packs the project's JVM application as a Docker image."
            inputs.file(distTar.outputs.files.singleFile)
            doFirst {
                applicationName.set(project.name)
                addFile(distTar.outputs.files.singleFile)
                val installDir = "/${distTar.archiveFileName.get()}.${distTar.archiveExtension.get()}"
                entryPoint(listOf("$installDir/bin/${project.name}"))
            }
        }
        logger.info("Adding docker task 'distDocker'")
    }

    private fun createExtension(project: Project): DockerPluginExtension {
        val extension = project.extensions.create(EXTENSION_NAME, DockerPluginExtension::class.java)

        logger.info("Adding docker extension")
        return extension
    }

    companion object {
        private val logger: Logger = Logging.getLogger(DockerPlugin::class.java)
        const val EXTENSION_NAME = "docker"
    }
}
