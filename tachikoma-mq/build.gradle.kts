plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation(project(":tachikoma-internal-api"))

    implementation("com.rabbitmq:amqp-client")
    implementation("com.google.guava:guava")
    implementation("io.grpc:grpc-protobuf")
}
