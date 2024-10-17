import io.ebean.gradle.EnhancePluginExtension

plugins {
    `tachikoma-kotlin`
    id("io.ebean")
    `kotlin-kapt`
}

dependencies {
    kapt("io.ebean:kotlin-querybean-generator")

    api("io.ebean:ebean-api")

    implementation(project(":tachikoma-internal-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.guava:guava")
    implementation("io.ebean:ebean")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

val ebeanVersion: String by project

extensions.configure<EnhancePluginExtension> {
//    debugLevel = 10
    kotlin = true
//    queryBeans = true
    generatorVersion = ebeanVersion
}

kapt {
//    correctErrorTypes = true
//    generateStubs = true
}
