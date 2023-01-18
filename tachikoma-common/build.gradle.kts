plugins {
    `tachikoma-kotlin`
    `kotlin-kapt`
}
dependencies {
    kapt("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-core")
    api("com.google.guava:guava")
    api("org.apache.logging.log4j:log4j-api-kotlin")
    api("org.kodein.di:kodein-di")
}
