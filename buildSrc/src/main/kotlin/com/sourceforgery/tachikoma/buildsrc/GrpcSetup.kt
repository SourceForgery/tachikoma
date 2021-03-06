package com.sourceforgery.tachikoma.buildsrc

import api
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import grpcKotlinVersion
import grpcVersion
import implementation
import jakartaAnnotationsVersion
import kotlinCoroutineVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.project
import protocVersion
import sourceSets
import java.io.File
import java.net.URL

fun Project.grpcSetup() {
    apply(plugin = "com.google.protobuf")
    apply(plugin = "kotlin")
    javaSetup()

    val downloadProtocLint: DownloadFileTask = rootProject.tasks.findByName("downloadProtocLint") as? DownloadFileTask
        ?: rootProject.tasks.create("downloadProtocLint", DownloadFileTask::class.java) {
            url = { URL("https://github.com/ckaznocha/protoc-gen-lint/releases/download/v0.2.1/protoc-gen-lint_$nativePrefix.zip") }
            outputFile = File(rootProject.buildDir, "protoc-gen-lint")
            zipFileMatcher = { "protoc-gen-lint" == it.name }
        }

    dependencies {
        implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutineVersion"))

        api("io.grpc:grpc-protobuf:$grpcVersion")

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")
        implementation(project(":tachikoma-protobuf-annotations"))
        implementation("io.grpc:grpc-stub:$grpcVersion")
        implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
        implementation("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationsVersion")
    }


    sourceSets {
        "main" {
            resources {
                srcDir("src/main/proto")
            }
        }
    }

    protobuf {
        protoc {
            // The artifact spec for the Protobuf Compiler
            artifact = "com.google.protobuf:protoc:$protocVersion"
        }

        plugins {
            id("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
            }
            id("grpckt") {
                artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
            }
            id("lint") {
                path = downloadProtocLint.outputFile.absolutePath
            }
        }

        generateProtoTasks {
            all().configureEach {
                plugins {
                    id("grpc") {}
                    id("grpckt") {}
                    id("lint") {
                        option("sort_imports")
                    }
                }
            }
        }
    }
    afterEvaluate {
        tasks["generateProto"].dependsOn(downloadProtocLint)
    }
    fixUglyCode()
}
