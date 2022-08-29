import com.google.protobuf.gradle.protobuf

plugins {
    `tachikoma-grpc`
}

evaluationDependsOn(":tachikoma-frontend-api-proto")

dependencies {
    protobuf(project(":tachikoma-frontend-api-proto"))
}
