plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation(project(":tachikoma-rest"))
    implementation(project(":tachikoma-database"))
    implementation(project(":tachikoma-grpc"))
    implementation(project(":tachikoma-mq"))
    implementation(project(":tachikoma-common"))

    implementation("org.postgresql:postgresql:$postgresqlDriverVersion")
}

tasks["compileJava"].apply {
    this as JavaCompile
    options.compilerArgs.add("-Aorg.glassfish.hk2.metadata.location=META-INF/hk2-locator/com.sourceforgery.tachikoma")
}
