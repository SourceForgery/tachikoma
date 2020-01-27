applyKotlin()
apply(plugin = "application")

dependencies {
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.netty:netty-tcnative-boringssl-static:$nettyVersionSsl:$googleNativePrefix")
    implementation("com.github.jnr:jnr-unixsocket:0.18")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    implementation("io.grpc:grpc-stub:$grpcVersion")
}

val applicationMainClassName = "TracerKt"
extensions.configure<JavaApplication>("application") {
    mainClassName = applicationMainClassName
}

val fatJar by tasks.registering(Jar::class) {
    manifest {
        attributes.putAll(
            mapOf(
                "Implementation-Title" to "Tachikoma ESP Webserver",
                "Implementation-Version" to archiveVersion.get(),
                "Main-Class" to applicationMainClassName
            )
        )
    }
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
    archiveBaseName.set(project.name + "-all")
    configurations["runtimeClasspath"].forEach {
        from(
            if (it.isDirectory()) {
                it
            } else {
                zipTree(it)
            }
        )
    }
    with(tasks["jar"] as CopySpec)
}

tasks["assemble"].dependsOn(fatJar)