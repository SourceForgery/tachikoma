import com.google.protobuf.gradle.protobuf

applyGrpc()

dependencies {
    protobuf(project(":tachikoma-frontend-api-proto"))
}

rootProject.publishing {
    publications {
        create<MavenPublication>("TachikomaFrontendApiJvm") {
            from(components["java"])
            artifactId = project.name

            artifact(tasks["sourceJar"]) {
                classifier = "sources"
            }
            artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }
        }
    }
}
