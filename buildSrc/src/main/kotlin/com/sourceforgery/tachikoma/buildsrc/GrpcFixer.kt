package com.sourceforgery.tachikoma.buildsrc

import com.google.protobuf.gradle.GenerateProtoTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import java.io.File


private val GRPC_JAVA_CLASS_FINDER = Regex("^(public {2}final class)", RegexOption.MULTILINE)
private val GRPC_JAVA_FOR_NUMBER_FINDER = Regex("""^( *)(public static .* forNumber\(int value\) \{)""", RegexOption.MULTILINE)

/**
 * Add NonnullByDefault to all grpc packages
 * Ignore deprecations in generated code
 */
internal fun Project.fixUglyCode() {
    tasks.withType<GenerateProtoTask> {
        doLast {
            outputs.files.forEach { dir ->
                val javaDir = File(dir, "java")
                if (javaDir.isDirectory) {
                    val javaDirLength = javaDir.path.length
                    val javaDirs = javaDir.canonicalFile
                        .walk()
                        .filter { it.extension == "java" && it.isFile }
                        .onEach {
                            val contents = it.readText()
                            val newContents = contents.replace(GRPC_JAVA_CLASS_FINDER) { match ->
                                val declaration = match.groupValues.first()
                                """
                                    |@SuppressWarnings("deprecation")
                                    |$declaration
                                """.trimMargin()
                            }.replace(GRPC_JAVA_FOR_NUMBER_FINDER) { match ->
                                val indent = match.groupValues[1]
                                val declaration = match.groupValues[2]
                                """
                                    |$indent@javax.annotation.Nullable
                                    |$indent$declaration
                                """.trimMargin()
                            }
                            it.writeText(newContents)
                        }
                        .map { it.parentFile }
                        .toSet()

                    javaDirs
                        .forEach {
                            val packages = it.path.substring(javaDirLength).trim('/').replace("/", ".")
                            val x = File(it, "package-info.java")
                            if (packages.isNotEmpty()) {
                                x.writeText(
                                    """
                                        @NonnullByDefault
                                        package $packages;

                                        import com.sourceforgery.tachikoma.annotations.NonnullByDefault;
                                    """.trimIndent()
                                )
                            }
                        }
                }
            }
        }
    }
}
