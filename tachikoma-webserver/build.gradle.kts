applyKotlin()
apply(plugin = "application")

dependencies {
    implementation(project(":tachikoma-grpc"))
    implementation(project(":tachikoma-rest"))
    implementation(project(":tachikoma-common"))
    implementation(project(":tachikoma-startup"))
    implementation(project(":tachikoma-internal-api"))
    implementation(project(":tachikoma-database"))
    implementation(project(":tachikoma-mq"))
    implementation("io.ebean:ebean:$ebeanVersion")
    implementation("com.linecorp.armeria:armeria:$armeriaVersion")
    implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
}

rootProject.extensions.configure<co.riiid.gradle.GithubExtension>("github") {
    addAssets("$buildDir/distributions/tachikoma-webserver-${project.version}.tar")
}

val startClass = "com.sourceforgery.tachikoma.webserver.MainKt"

extensions.configure<JavaApplication>("application") {
    mainClass.set(startClass)
}

val runLocalServer by tasks.creating(type = JavaExec::class) {
    description = "Development"
    dependsOn("assemble")
    afterEvaluate {
        main = startClass
        classpath = project.sourceSets["main"].runtimeClasspath
        jvmArgs(
            listOf(
                "-DtachikomaConfig=${System.getProperty("user.home")}/.tachikoma.properties"
            )
        )
    }
}

tasks.getByPath(":githubRelease").dependsOn(tasks[ApplicationPlugin.TASK_DIST_TAR_NAME])

tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME].enabled = false
