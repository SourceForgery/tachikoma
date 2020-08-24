applyKotlin()

dependencies {
    implementation(project(":tachikoma-internal-api"))

    implementation("com.rabbitmq:amqp-client:5.9.0")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("org.glassfish.hk2.external:jakarta.inject:$hk2Version")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
}
