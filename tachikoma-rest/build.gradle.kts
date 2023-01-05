plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation("com.linecorp.armeria:armeria")
    implementation("com.linecorp.armeria:armeria-kotlin")
    implementation("com.sun.mail:jakarta.mail")
    implementation("io.grpc:grpc-protobuf")

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-html")
}
