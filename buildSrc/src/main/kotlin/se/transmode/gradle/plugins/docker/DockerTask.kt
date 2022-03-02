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

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import se.transmode.gradle.plugins.docker.client.DockerClient
import se.transmode.gradle.plugins.docker.client.NativeDockerClient
import se.transmode.gradle.plugins.docker.image.Dockerfile
import java.io.File
import java.nio.file.Files

@Suppress("LeakingThis")
abstract class DockerTask : DefaultTask() {
    // full path to the docker executable
    @get:Input
    abstract val dockerBinary: Property<String>

    // Name of the application being wrapped into a docker image (default: project.name)
    @get:Input
    abstract val applicationName: Property<String>

    // Which version to use along with the tag (default: ${project.version})
    @get:Input
    abstract val tagVersion: Property<String>

    // Whether or not to execute docker to build the image (default: false)
    @get:Input
    abstract val dryRun: Property<Boolean>

    // Whether or not to push the image into the registry (default: false)
    @get:Input
    abstract val push: Property<Boolean>

    // Hostname, port of the docker image registry unless Docker index is used
    @get:Input
    @get:Optional
    abstract val registry: Property<String>

    /**
     * Path to external Dockerfile
     */
    @get:Input
    @get:Optional
    abstract val dockerfile: RegularFileProperty

    /**
     * Name of the base docker image
     */
    @get:Input
    abstract val baseImage: Property<String>

    // Dockerfile instructions (ADD, RUN, etc.)
    @Input
    val instructions = mutableListOf<String>()

    // Dockerfile staging area i.e. context dir
    @get:Internal
    abstract val stageDir: DirectoryProperty

    // Tasks necessary to setup the stage before building an image
    @Internal
    val stageBacklog = mutableListOf<Runnable>()

    // Should we use Docker's remote API instead of the docker executable
    @get:Input
    abstract val useApi: Property<Boolean>

    // URL of the remote Docker host (default: localhost)
    @get:Input
    @get:Optional
    abstract val hostUrl: Property<String>

    // The full imageTag without version. This defaults to (depending on the data available):
    // $registry/$applicationName
    // ${project.group}/$applicationName
    @get:Input
    abstract val imageTag: Property<String>

    // Docker remote API credentials
    @get:Input
    @get:Optional
    abstract val apiUsername: Property<String>

    @get:Input
    @get:Optional
    abstract val apiPassword: Property<String>

    @get:Input
    @get:Optional
    abstract val apiEmail: Property<String>

    init {
        val extension = project.extensions[DockerPlugin.EXTENSION_NAME] as DockerPluginExtension
        applicationName.convention(project.provider { project.name })
        imageTag.convention(
            applicationName.map { applicationName ->
                val version = tagVersion.getOrElse(project.version.toString())
                if (registry.isPresent) {
                    "${registry.get()}/$applicationName:$version"
                } else if (project.group != "") {
                    "${project.group}/$applicationName:$version"
                } else {
                    applicationName
                }
            }
        )
        dryRun.convention(false)
        push.convention(false)
        baseImage.convention(extension.baseImage)
        dockerBinary.convention(extension.dockerBinary)
        registry.convention(extension.registry)
        useApi.convention(extension.useApi)
        hostUrl.convention(extension.hostUrl)
        apiUsername.convention(extension.apiUsername)
        apiPassword.convention(extension.apiPassword)
        apiEmail.convention(extension.apiEmail)
        stageDir.convention(project.layout.buildDirectory.dir("docker"))
        tagVersion.convention(project.provider { project.version.toString() })
    }

    fun addFile(source: String, destination: String = "/") {
        addFile(project.file(source), destination)
    }

    fun addFile(source: File, destination: String = "/") {
        stageBacklog.add {
            var target: File = stageDir.get().asFile
            if (source.isDirectory) {
                target = File(stageDir.get().asFile, source.name)
            }
            project.copy {
                from(source)
                into(target)
            }
        }
        instructions.add("""ADD "${source.name}" "$destination"""")
    }

    fun addFile(copySpec: Action<CopySpec>) {
        val fileName = "add_${instructions.size + 1}.tar"
        stageBacklog.add {
            val tarFile = stageDir.get().file(fileName).asFile
            createTarArchive(tarFile, project.copySpec(copySpec))
        }
        instructions.add("ADD $fileName /")
    }

    fun addFile(copySpec: CopySpec) {
        val fileName = "add_${instructions.size + 1}.tar"
        stageBacklog.add {
            val tarFile = stageDir.get().file(fileName).asFile
            createTarArchive(tarFile, copySpec)
        }
        instructions.add("ADD $fileName /")
    }

    fun addFilesNoExtract(sources: Iterable<File>, renamer: (String) -> String) {
        addFile {
            includeEmptyDirs = false
            eachFile {
                path = renamer(path)
            }
            from(sources)
        }
    }

    fun addFiles(sources: Iterable<File>, renamer: (String) -> String) {
        addFile {
            includeEmptyDirs = false
            eachFile {
                path = renamer(path)
            }
            for (source in sources) {
                from(project.toTree(source))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun from(image: String, name: String?) {
        if (name?.isNotBlank() == true) {
            instructions.add("FROM $image AS $name")
        } else {
            instructions.add("FROM $image")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun copy(src: String, dest: String, from: String?) {
        instructions.add(
            if (from?.isNotBlank() == true) {
                "COPY --from=$from $src $dest"
            } else {
                "COPY $src $dest"
            }
        )
    }

    fun createTarArchive(tarFile: File, copySpec: CopySpec) {
        val tmpDir = Files.createTempDirectory(null)
            .toFile()
        try {
            logger.info("Creating tar archive {} from {}", tarFile, tmpDir)
            /* copy all files to temporary directory */
            project.copy {
                with(
                    project.copySpec {
                        into("/") {
                            with(copySpec)
                        }
                    }
                )
                into(tmpDir)
            }
            TarArchiveOutputStream(tarFile.outputStream().buffered(), "UTF-8").use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                tmpDir.walk()
                    .filter { it.isFile }
                    .forEach { file ->
                        tar.putArchiveEntry(
                            tar.createArchiveEntry(file, file.toRelativeString(tmpDir))
                        )
                        file.inputStream().use {
                            it.copyTo(tar)
                        }
                        tar.closeArchiveEntry()
                    }
            }
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    fun workingDir(wd: String) {
        instructions.add("WORKDIR $wd")
    }

    fun addInstruction(cmd: String, value: String) {
        instructions.add("$cmd $value")
    }

    fun runCommand(command: String) {
        instructions.add("RUN $command")
    }

    fun exposePort(port: Int) {
        instructions.add("EXPOSE $port")
    }

    fun setEnvironment(key: String, value: String) {
        instructions.add("ENV $key $value")
    }

    fun setTagVersionToLatest() {
        tagVersion.set("latest")
    }

    private fun List<String>.quote() =
        joinToString("\", \"", prefix = "\"", postfix = "\"")

    fun volume(vararg paths: String) {
        instructions.add("VOLUME [${paths.toList().quote()}]")
    }

    fun setEntryPoint(entryPoints: List<String>) {
        instructions.add("ENTRYPOINT [${entryPoints.quote()}]")
    }

    fun entryPoint(entryPoint: List<String>) {
        setEntryPoint(entryPoint)
    }

    fun setDefaultCommand(cmd: List<String>) {
        instructions.add("CMD [${cmd.quote()}]")
    }

    fun defaultCommand(cmd: List<String>) {
        setDefaultCommand(cmd)
    }

    private fun createDirIfNotExists(dir: File): File {
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    internal fun setupStageDir() {
        logger.info("Setting up staging directory.")
        createDirIfNotExists(stageDir.get().asFile)
        for (runnable in stageBacklog) {
            runnable.run()
        }
    }

    internal fun buildDockerfile(): Dockerfile {
        val baseDockerfile: Dockerfile
        val dockerfile = dockerfile
        baseDockerfile = if (dockerfile.isPresent) {
            logger.info("Creating Dockerfile from file {}.", dockerfile)
            Dockerfile.fromExternalFile(dockerfile.get().asFile)
        } else {
            logger.info("Creating Dockerfile from base {}.", baseImage)
            Dockerfile.fromBaseImage(baseImage.get())
        }
        return baseDockerfile.appendAll(instructions)
    }

    @TaskAction
    fun build() {
        setupStageDir()
        buildDockerfile().writeToFile(File(stageDir.get().asFile, "Dockerfile"))
        val tag = imageTag.get()
        logger.info("Determining image tag: {}", tag)
        if (!dryRun.get()) {
            val client = client
            client.buildImage(stageDir.get().asFile, tag)
            if (push.get()) {
                client.pushImage(tag)
            }
        }
    }

    private val client: DockerClient
        get() {
            val client: DockerClient
            if (useApi.get()) {
                TODO("Not implemented")
//                logger.info("Using the Docker remote API.")
//                client = JavaDockerClient.create(
//                    getHostUrl(),
//                    getApiUsername(),
//                    getApiPassword(),
//                    getApiEmail()
//                )
            } else {
                logger.info("Using the native docker binary.")
                client = NativeDockerClient(dockerBinary.get())
            }
            return client
        }
}

fun Project.toTree(it: File): FileTree {
    return when {
        it.isDirectory -> fileTree(it)
        it.name.endsWith("tar.gz") ||
            it.name.endsWith("tar") -> tarTree(it)
        it.name.endsWith("zip") -> zipTree(it)
        else -> TODO("Don't know how to get a tree from $it")
    }
}
