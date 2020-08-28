applyKotlin()

System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")

dependencies {
    testImplementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    testImplementation(project(":tachikoma-database"))
    testImplementation(project(":tachikoma-grpc"))
    testImplementation(project(":tachikoma-rest"))
    testImplementation(project(":tachikoma-webserver"))

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("com.google.guava:guava:$guavaVersion")
    testImplementation("com.google.protobuf:protobuf-java-util:$protocVersion")
    testImplementation("com.google.protobuf:protobuf-java:$protocVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("com.opentable.components:otj-pg-embedded:$pgEmbeddedVersion")
    testImplementation("io.ebean:ebean:$ebeanVersion")
    testImplementation("io.grpc:grpc-stub:$grpcVersion")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testImplementation("com.linecorp.armeria:armeria:$armeriaVersion")
    testImplementation("com.squareup.okhttp3:okhttp:4.4.0")
    testImplementation("org.jsoup:jsoup:1.12.2")
    testImplementation("com.sun.mail:javax.mail:$javaxMailVersion")
}
