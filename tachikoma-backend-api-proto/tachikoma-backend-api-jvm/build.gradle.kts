import com.google.protobuf.gradle.protobuf

applyGrpc()

evaluationDependsOn(":tachikoma-backend-api-proto")

dependencies {
    protobuf(project(":tachikoma-backend-api-proto"))
}

@Suppress("UnstableApiUsage")
tasks.getByName("processResources", ProcessResources::class) {
    exclude("**/*.proto")
}
