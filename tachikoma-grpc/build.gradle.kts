plugins {
    `tachikoma-kotlin`
}

evaluationDependsOn(":tachikoma-backend-api-proto")
evaluationDependsOn(":tachikoma-frontend-api-proto")

dependencies {
    implementation("com.google.protobuf:protobuf-java-util")
    implementation("com.linecorp.armeria:armeria")
    implementation("com.linecorp.armeria:armeria-grpc")
    implementation("com.linecorp.armeria:armeria-kotlin")
    implementation("com.sun.mail:jakarta.mail")
    implementation("com.samskivert:jmustache")
    implementation("io.grpc:grpc-kotlin-stub")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("commons-lang:commons-lang")
    implementation("org.jsoup:jsoup")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))

    testImplementation("com.sun.mail:jakarta.mail")
    testImplementation("io.mockk:mockk")
}
