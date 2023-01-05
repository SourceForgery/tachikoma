plugins {
    `tachikoma-kotlin`
}

System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")

dependencies {
    testImplementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))
    testImplementation(project(":tachikoma-database"))
    testImplementation(project(":tachikoma-grpc"))
    testImplementation(project(":tachikoma-rest"))
    testImplementation(project(":tachikoma-webserver"))
    testImplementation(project(":tachikoma-startup"))

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("com.google.guava:guava")
    testImplementation("com.google.protobuf:protobuf-java-util")
    testImplementation("com.google.protobuf:protobuf-java")
    testImplementation("com.h2database:h2")
    testImplementation("com.opentable.components:otj-pg-embedded")
    testImplementation("io.ebean:ebean")
    testImplementation("io.ebean:ebean-ddl-generator")
    testImplementation("io.grpc:grpc-stub")
    testImplementation("io.mockk:mockk")
    testImplementation("org.apache.logging.log4j:log4j-core")
    testImplementation("org.apache.logging.log4j:log4j-jul")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl")
    testImplementation("com.linecorp.armeria:armeria")
    testImplementation("com.linecorp.armeria:armeria-kotlin")
    testImplementation("com.squareup.okhttp3:okhttp")
    testImplementation("org.jsoup:jsoup")
    testImplementation("com.sun.mail:jakarta.mail")
}
