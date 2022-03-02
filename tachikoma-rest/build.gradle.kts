plugins {
    id("tachikoma.kotlin")
}

dependencies {
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-kotlin:$armeriaVersion")
    implementation("com.sun.mail:jakarta.mail:$jakartaMailVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
}
