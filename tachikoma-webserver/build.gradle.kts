applyKotlin()
apply(plugin = "application")

dependencies {
    implementation(project(":tachikoma-grpc"))
    implementation(project(":tachikoma-rest"))
    implementation(project(":tachikoma-common"))
    implementation(project(":tachikoma-startup"))
    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database"))
    implementation(project(":tachikoma-mq"))
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("org.glassfish.hk2.external:javax.inject:$hk2Version")
}

rootProject.extensions.configure<co.riiid.gradle.GithubExtension>("github") {
    setAssets(*(assets + arrayOf("$buildDir/distributions/tachikoma-webserver-${project.version}.tar")))
}

extensions.configure<JavaApplication>("application") {
    mainClassName = "com.sourceforgery.tachikoma.webserver.MainKt"
}

tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME].enabled = false