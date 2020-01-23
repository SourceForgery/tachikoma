applyJava()

dependencies {
    implementation("com.google.code.findbugs:jsr305:$jsr305Version")
}

rootProject.publishing {
    publications {
        create<MavenPublication>("ProtobufAnnotationsJvm") {
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

bintray {
    addPublications("ProtobufAnnotationsJvm")
}