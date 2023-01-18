plugins {
    `tachikoma-kotlin`
}

dependencies {
    api(project(":tachikoma-internal-api"))
    api(project(":tachikoma-database-api"))

    implementation("org.apache.logging.log4j:log4j-api-kotlin")
    implementation("io.ebean:ebean")
    implementation("io.ebean:ebean-querybean")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.apache.logging.log4j:log4j-iostreams")
    implementation("net.bytebuddy:byte-buddy")
}
