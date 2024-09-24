package com.sourceforgery.tachikoma.buildsrc

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

open class DuplicateClassesExtension {
    // List of duplicates, e.g.
    // acceptedDuplicates = mutableListOf(
    //     mutableListOf("mongodb-driver-3.6.1", "fongo-2.2.0-RC2")
    // )
    var acceptedDuplicates: MutableList<MutableList<String>> = mutableListOf(mutableListOf())
    var ignoredFiles = mutableListOf(
        Regex("^module-info.class$"),
        Regex(".*/package-info.class$"),
        Regex("^META-INF/.*"),
        // Ignore netty binary dependencies
        Regex("^io/netty/channel/unix/.*"),
    )
}

@Suppress("UnstableApiUsage")
val Project.duplicateClassesChecker: DuplicateClassesExtension
    get() =
        extensions.findByType(DuplicateClassesExtension::class.java)
            ?: extensions.create("duplicateClassesChecker")
