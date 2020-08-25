applyKotlin()

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("org.jsoup:jsoup:1.12.2")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.glassfish.hk2.external:jakarta.inject:$hk2Version")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")

    implementation(project(":jersey-uri-builder"))
    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
