plugins {
    `tachikoma-kotlin`
    `kotlin-kapt`
}
dependencies {
    kapt("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    api("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
    api("org.kodein.di:kodein-di:$kodeinVersion")
}
