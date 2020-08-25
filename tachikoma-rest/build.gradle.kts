applyKotlin()

dependencies {
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("org.glassfish.hk2.external:jakarta.inject:$hk2Version")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
