applyKotlin()

dependencies {
    implementation("io.ebean:ebean-annotation:$ebeanAnnotationVersion")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.glassfish.hk2.external:jakarta.inject:$hk2Version")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")

    api(project(":tachikoma-common"))
    api(project(":tachikoma-frontend-api-proto:tachikoma-frontend-api-jvm"))
    api(project(":tachikoma-internal-proto-jvm"))
}
