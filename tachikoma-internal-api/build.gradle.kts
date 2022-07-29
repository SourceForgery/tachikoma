plugins {
    `tachikoma-kotlin`
}

dependencies {
    implementation("io.ebean:ebean-annotation:$ebeanAnnotationVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-kotlin:$armeriaVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutineVersion")
    implementation("com.sun.mail:jakarta.mail:$jakartaMailVersion")

    api(project(":tachikoma-common"))
    api(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    api(project(":tachikoma-internal-proto-jvm"))
}
