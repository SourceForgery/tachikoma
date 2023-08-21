import org.gradle.plugins.ide.idea.model.IdeaModel
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf
import com.sourceforgery.tachikoma.buildsrc.DownloadFileTask
import com.sourceforgery.tachikoma.buildsrc.fixUglyCode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.net.URL

plugins {
    id("com.google.protobuf")
    kotlin("jvm")
    id("tachikoma-kotlin")
    `idea`
}

val javaVersion: String by project

val downloadProtocLint: DownloadFileTask = rootProject.tasks.findByName("downloadProtocLint") as? DownloadFileTask
    ?: rootProject.tasks.create("downloadProtocLint", DownloadFileTask::class.java) {
        url =
            { URL("https://github.com/ckaznocha/protoc-gen-lint/releases/download/v0.3.0/protoc-gen-lint_$nativePrefix.zip") }
        outputFile = File(rootProject.buildDir, "protoc-gen-lint")
        zipFileMatcher = { "protoc-gen-lint" == it.name }
    }

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom"))

    api("io.grpc:grpc-protobuf")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation(project(":tachikoma-protobuf-annotations"))
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-kotlin-stub")
    implementation("jakarta.annotation:jakarta.annotation-api")
}

extensions.getByType<IdeaModel>().apply {
    module {
        outputDir = file("build/idea-out")
        testOutputDir = file("build/idea-testout")
        generatedSourceDirs.add(file("$buildDir/generated/source/proto/main/java/"))
        generatedSourceDirs.add(file("$buildDir/generated/source/proto/main/grpc/"))

        // TODO Can this be removed?
        scopes["COMPILE"]!!["plus"]!!.add(configurations["protobuf"])
    }
}

protobuf {
    val protocVersion: String by project
    val grpcVersion: String by project
    val grpcKotlinVersion: String by project
    protoc {
        // The artifact spec for the Protobuf Compiler

        artifact = "com.google.protobuf:protoc:$protocVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
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

tasks.withType<Jar> {
    exclude("classpath.index")
}

afterEvaluate {
    tasks["generateProto"].dependsOn(downloadProtocLint)
}
fixUglyCode()
