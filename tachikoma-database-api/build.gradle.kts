import io.ebean.gradle.EnhancePluginExtension
applyKotlin()
apply(plugin = "io.ebean")
// apply(plugin = "kotlin-kapt")

dependencies {
    implementation(project(":tachikoma-internal-api"))

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

//    add("kapt", "io.ebean:kotlin-querybean-generator:$querybeanVersion")

    implementation("io.ebean:ebean:$ebeanVersion")
//    implementation("io.ebean:ebean-querybean:$querybeanVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

extensions.getByType(EnhancePluginExtension::class.java).apply {
    debugLevel = 0
}