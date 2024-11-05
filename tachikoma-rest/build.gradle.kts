plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))

    implementation("com.linecorp.armeria:armeria")
    implementation("com.linecorp.armeria:armeria-kotlin")
    implementation("io.grpc:grpc-protobuf")
    implementation("org.eclipse.angus:jakarta.mail")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-html")
}
