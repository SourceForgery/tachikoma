package com.sourceforgery.tachikoma.buildsrc

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate // ktlint-disable
import java.io.File
import java.util.zip.ZipFile

/**
 * Checks whether the artifacts of the configurations of the project contain the same classes.
 */
@Suppress("UnstableApiUsage")
open class CheckDuplicateClassesTask : DefaultTask(), VerificationTask {
    private var ignoreFailures: Boolean = false

    override fun getIgnoreFailures() = this.ignoreFailures
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        this.ignoreFailures = ignoreFailures
    }

    init {
        val check by project.tasks
        @Suppress("LeakingThis")
        check.dependsOn(this)

        getCompileJava()?.let {
            dependsOn(it)
        }

        getCompileTestJava()?.let {
            dependsOn(it)
        }
    }

    private fun getCompileJava(): AbstractCompile? =
        project.tasks.findByName("compileJava") as? AbstractCompile

    private fun getCompileTestJava(): AbstractCompile? =
        project.tasks.findByName("compileTestJava") as? AbstractCompile

    @TaskAction
    fun checkForDuplicateClasses() {
        val result = StringBuilder()

        result.append(checkConfiguration(getCompileJava()))
        result.append(checkConfiguration(getCompileTestJava()))

        if (result.isNotEmpty()) {
            val message = StringBuilder("There are conflicting files in the following tasks")

            if (project.gradle.startParameter.logLevel > LogLevel.INFO) {
                message.append(" (add --info for details)")
            }

            message.append(":$result")

            if (ignoreFailures) {
                logger.warn(message.toString())
            } else {
                throw GradleException(message.toString())
            }
        }
    }

    private fun checkConfiguration(compileTask: AbstractCompile?): String {
        if (compileTask == null) {
            return ""
        }

        logger.info("Checking for duplicates in ${compileTask.path}")

        val jarsByFile = MultimapBuilder.hashKeys().treeSetValues().build<String, String>()

        val classpath = compileTask.classpath.toList() + compileTask.outputs.files

        classpath.forEach { entry ->
                logger.debug("    '$entry'")

            val ignoredFiles = project.duplicateClassesChecker.ignoredFiles
                if (entry.isFile && (
                        entry.name.endsWith("zip") ||
                            entry.name.endsWith("jar")
                        )
                ) {
                    try {
                        ZipFile(entry).use { zip ->
                            zip
                                .entries()
                                .asSequence()
                                .filterNot { it.isDirectory }
                                .filter { it.name.endsWith(".class") }
                                .filter { entry ->
                                    ignoredFiles.none { regex ->
                                        regex.matches(entry.name)
                                    }
                                }
                                .forEach { jarsByFile.put(it.name, entry.path) }
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Failed to open zip $entry", e)
                    }
                } else if (entry.isDirectory) {
                    val directory = entry.absoluteFile.canonicalFile
                    directory.walk()
                        .onEnter {
                            it == directory || !it.path.substring(directory.path.length).startsWith("META-INF/")
                        }
                        .filter { !it.isDirectory }
                        .filter { it.name.endsWith(".class") }
                        .map { it.path.substring(directory.path.length) }
                        .filter { relativePath ->
                            ignoredFiles.none { regex ->
                                regex.matches(relativePath)
                            }
                        }
                        .forEach { jarsByFile.put(it, entry.path) }
                } else if (entry.exists() && !ignoredExtensions.contains(entry.extension)) {
                    logger.warn("Don't know what to do with dependency $entry")
                }
            }

        val duplicateFiles = MultimapBuilder.hashKeys().treeSetValues().build<String, String>()
        val allowedDupeFiles = MultimapBuilder.hashKeys().treeSetValues().build<String, String>()
        for (depList in project.duplicateClassesChecker.acceptedDuplicates) {
            for (dep in depList) {
                allowedDupeFiles.putAll(dep, depList)
            }
        }

        jarsByFile
            .asMap()
            .entries
            .filter { it.value.size > 1 }
            // Filter out all files where ALL dupes are listed
            .filterNot {
                val values = it.value.map { File(it).nameWithoutExtension }
                val sortedSet = allowedDupeFiles[values.first()]
                sortedSet.containsAll(values)
            }
            .forEach { duplicateFiles.putAll(it.key, it.value) }
        if (duplicateFiles.size() != 0) {
            logger.info(buildMessageWithConflictingClasses(duplicateFiles))
            return "\n\n${compileTask.path}\n${buildMessageWithUniqueModules(duplicateFiles.asMap().values)}"
        }

        return ""
    }

    companion object {
        private val ignoredExtensions = setOf("exe", "gz", "tar")

        fun buildMessageWithConflictingClasses(duplicateFiles: Multimap<String, String>): String {
            val conflictingClasses = MultimapBuilder.hashKeys().treeSetValues().build<String, String>()

            duplicateFiles
                .entries()
                .forEach { conflictingClasses.put(it.value, it.key) }
            val message = StringBuilder()
            conflictingClasses.asMap().forEach {
                message.append("\n    Found duplicate classes in ${it.key}:\n        ${it.value.joinToString("\n        ")}")
            }

            return message.toString()
        }

        fun buildMessageWithUniqueModules(conflictingModules: Collection<Collection<String>>): String {
            val moduleMessages = mutableListOf<String>()

            conflictingModules.forEach { modules ->
                val message = "    ${joinModules(modules)}"
                if (!moduleMessages.contains(message)) {
                    moduleMessages.add(message)
                }
            }

            return moduleMessages.joinToString("\n")
        }

        private fun joinModules(modules: Collection<String>): String {
            return modules.joinToString(", ")
        }
    }
}