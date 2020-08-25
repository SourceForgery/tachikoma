applyKotlin()

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("org.jsoup:jsoup:1.12.2")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutineVersion")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
