applyKotlin()
apply(plugin = "application")
apply(plugin = "com.google.osdetector")

dependencies {
    implementation("com.github.jnr:jnr-unixsocket:0.25")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.netty:netty-tcnative-boringssl-static:$nettyVersionSsl:$googleNativePrefix")
    implementation("net.sf.expectit:expectit-core:0.9.0")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
    implementation("com.google.protobuf:protobuf-java:$protocVersion")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("io.grpc:grpc-stub:$grpcVersion")

    implementation(project(":tachikoma-common"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
}

extensions.configure<JavaApplication>("application") {
    mainClassName = "com.sourceforgery.tachikoma.postfix.MainKt"
}

rootProject.extensions.configure<co.riiid.gradle.GithubExtension> {
    addAssets("$buildDir/distributions/tachikoma-postfix-utils-${project.version}.tar")
}

tasks.getByPath(":githubRelease").dependsOn(ApplicationPlugin.TASK_DIST_TAR_NAME)

tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME].enabled = false