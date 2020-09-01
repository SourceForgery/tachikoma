import io.ebean.gradle.EnhancePluginExtension

applyKotlin()
apply(plugin = "io.ebean")
apply(plugin = "kotlin-kapt")

dependencies {
    add("kapt", "io.ebean:kotlin-querybean-generator:$querybeanVersion")

    implementation(project(":tachikoma-internal-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("io.ebean:ebean:$ebeanVersion")
    implementation("io.ebean:ebean-querybean:$querybeanVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

extensions.getByType(EnhancePluginExtension::class.java).apply {
    debugLevel = 0
}
