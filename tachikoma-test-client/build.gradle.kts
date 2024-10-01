plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("com.github.jnr:jnr-unixsocket")
    implementation("com.google.protobuf:protobuf-java-util")
    implementation(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    implementation(project(":tachikoma-postfix-utils"))
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-kotlin-stub")
    implementation("com.linecorp.armeria:armeria-grpc")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-jul")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
    implementation("jakarta.mail:jakarta.mail-api")
    implementation(project(":jersey-uri-builder"))
}
