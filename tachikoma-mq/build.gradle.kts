plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation(project(":tachikoma-internal-api"))

    implementation("com.rabbitmq:amqp-client:5.14.2")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
}
