applyKotlin()

dependencies {
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
