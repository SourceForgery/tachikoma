plugins {
    id("tachikoma.kotlin")
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("com.github.jnr:jnr-unixsocket:0.25")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.sun.mail:jakarta.mail:$jakartaMailVersion")
    implementation(project(":jersey-uri-builder"))
}
