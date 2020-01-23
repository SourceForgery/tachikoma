applyKotlin()

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
//    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("net.moznion:uribuilder-tiny:2.7.1")
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.glassfish.hk2.external:javax.inject:$hk2Version")
    implementation("org.glassfish.hk2:hk2-api:$hk2Version")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")

    implementation(project(":tachikoma-backend-api-proto:tachikoma-backend-api-jvm"))

    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database-api"))
}
