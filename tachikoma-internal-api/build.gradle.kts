plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation("io.ebean:ebean-annotation")
    implementation("com.google.guava:guava")
    implementation("com.linecorp.armeria:armeria")
    implementation("com.linecorp.armeria:armeria-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.eclipse.angus:jakarta.mail")

    api(project(":tachikoma-common"))
    api(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    api(project(":tachikoma-internal-proto-jvm"))
}
