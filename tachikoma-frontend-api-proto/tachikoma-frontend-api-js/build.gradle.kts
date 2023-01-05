import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf")
    id("java")
    kotlin("js")
}

kotlin {
    js {
        nodejs()
    }
}

dependencies {
    protobuf(project(":tachikoma-frontend-api-proto"))
    protobuf("com.google.protobuf:protobuf-java")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc"
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                // task.builtins {
                //     remove java
                //         js {}
                // }
            }
        }
    }
}

tasks.jar {
    exclude("**/*.class")
    exclude("**/*.proto")
    from("build/generated/source/proto/main/js/") {
        into("protobuf")
        include("**/*.js")
    }
}
