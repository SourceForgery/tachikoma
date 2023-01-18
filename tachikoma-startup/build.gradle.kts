plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation(project(":tachikoma-rest"))
    implementation(project(":tachikoma-database"))
    implementation(project(":tachikoma-grpc"))
    implementation(project(":tachikoma-mq"))
    implementation(project(":tachikoma-common"))

    implementation("org.postgresql:postgresql")
}
