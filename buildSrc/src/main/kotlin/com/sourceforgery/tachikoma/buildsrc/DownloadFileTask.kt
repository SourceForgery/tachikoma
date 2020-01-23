package com.sourceforgery.tachikoma.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

open class DownloadFileTask : DefaultTask() {
    @get:Input
    val nativePrefix: String = OperatingSystem.current().nativePrefix.replace('-', '_')

    @get:OutputFile
    lateinit var outputFile: File

    @get:Input
    var executableFile: Boolean = true

    // Only used when downloading zip archive
    @get:Internal
    var zipFileMatcher: (ZipEntry) -> Boolean = { true }

    @get:Input
    lateinit var url: DownloadFileTask.() -> URL

    @TaskAction
    fun downloadFile() {
        val generatedUrl = url()
        if (generatedUrl.path.endsWith(".zip")) {
            val tempDir = getTemporaryDir()

            val tempOutput = File(tempDir, "download.zip")
            tempOutput.outputStream().use { ut ->
                generatedUrl.openStream().use { it.copyTo(ut) }
            }
            val zip = ZipFile(tempOutput)
            val entry = zip
                .entries()
                .asSequence()
                .first(zipFileMatcher)

            outputFile.outputStream().use { ut ->
                zip.getInputStream(entry).use {
                    it.copyTo(ut)
                }
            }
        } else {
            outputFile.outputStream().use { ut ->
                generatedUrl.openStream().use { it.copyTo(ut) }
            }
        }
        if (executableFile) {
            outputFile.setExecutable(true, false)
        }
    }
}