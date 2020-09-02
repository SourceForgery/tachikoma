applyKotlin()
apply(plugin = "application")
apply(plugin = "com.google.osdetector")

dependencies {
    implementation("com.github.jnr:jnr-unixsocket:0.25")
    implementation("net.sf.expectit:expectit-core:0.9.0")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
    implementation("com.google.protobuf:protobuf-java:$protocVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-common"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    testImplementation("io.mockk:mockk:1.10.0")
}

extensions.configure<JavaApplication>("application") {
    mainClassName = "com.sourceforgery.tachikoma.postfix.MainKt"
}

rootProject.extensions.configure<co.riiid.gradle.GithubExtension> {
    addAssets("$buildDir/distributions/tachikoma-postfix-utils-${project.version}.tar")
}

rootProject.tasks["githubRelease"]
    .dependsOn(tasks[ApplicationPlugin.TASK_DIST_TAR_NAME])

tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME].enabled = false
