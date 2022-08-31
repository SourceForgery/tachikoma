plugins {
    `tachikoma-kotlin`
}

evaluationDependsOn(":tachikoma-backend-api-proto")
evaluationDependsOn(":tachikoma-frontend-api-proto")

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.10")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-kotlin:$armeriaVersion")
    implementation("com.sun.mail:jakarta.mail:$jakartaMailVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutineVersion")
    implementation("commons-lang:commons-lang:1.0.1")
    implementation("org.jsoup:jsoup:$jsoupVersion")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
