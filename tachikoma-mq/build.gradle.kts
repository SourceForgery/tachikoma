applyKotlin()

dependencies {
    implementation(project(":tachikoma-internal-api"))
//    implementation(project(":tachikoma-common"))

    implementation("com.rabbitmq:amqp-client:5.1.2")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("org.glassfish.hk2.external:javax.inject:$hk2Version")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
}