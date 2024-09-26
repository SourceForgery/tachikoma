plugins {
    `tachikoma-kotlin`
    application
    id("com.google.osdetector")
}

dependencies {
    implementation("com.github.jnr:jnr-unixsocket")
    implementation("net.sf.expectit:expectit-core")
    implementation("org.apache.logging.log4j:log4j-iostreams")
    implementation("com.google.protobuf:protobuf-java")
    implementation("io.grpc:grpc-kotlin-stub")
    implementation("com.linecorp.armeria:armeria-grpc")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-jul")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.squareup:tape")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-common"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    testImplementation("io.mockk:mockk")
}

extensions.configure<JavaApplication>("application") {
    mainClass.set("com.sourceforgery.tachikoma.postfix.MainKt")
}

rootProject.extensions.configure<com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension> {
    releaseAssets.from("build/distributions/tachikoma-postfix-utils-${project.version}.tar")
}

rootProject.tasks["githubRelease"]
    .dependsOn(tasks[ApplicationPlugin.TASK_DIST_TAR_NAME])

tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME].enabled = false
