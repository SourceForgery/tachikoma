import io.ebean.gradle.EnhancePluginExtension

plugins {
    `tachikoma-kotlin`
    id("io.ebean")
    `kotlin-kapt`
}

dependencies {
    add("kapt", "io.ebean:kotlin-querybean-generator")

    implementation(project(":tachikoma-internal-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.guava:guava")
    implementation("io.ebean:ebean")
    implementation("io.ebean:ebean-querybean")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

extensions.configure<EnhancePluginExtension> {
    debugLevel = 0
}
