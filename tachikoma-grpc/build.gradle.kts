applyKotlin()

evaluationDependsOn(":tachikoma-backend-api-proto")
evaluationDependsOn(":tachikoma-frontend-api-proto")

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-kotlin:$armeriaVersion")
    implementation("com.sun.mail:jakarta.mail:$jakartaMailVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutineVersion")
    implementation("org.jsoup:jsoup:1.12.2")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
