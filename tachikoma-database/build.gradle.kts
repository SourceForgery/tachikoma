plugins {
    id("tachikoma.kotlin")
}

dependencies {
    api(project(":tachikoma-internal-api"))
    api(project(":tachikoma-database-api"))

    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
    implementation("io.ebean:ebean:$ebeanVersion")
    implementation("io.ebean:ebean-querybean:$querybeanVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
    implementation("net.bytebuddy:byte-buddy:$bytebuddyVersion")
}
