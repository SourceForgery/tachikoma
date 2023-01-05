plugins {
    `tachikoma-java`
}

dependencies {
    val jsr305Version: String by project
    implementation("com.google.code.findbugs:jsr305:$jsr305Version")
}
