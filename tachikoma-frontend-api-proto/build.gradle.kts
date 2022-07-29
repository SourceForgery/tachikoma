plugins {
    `tachikoma-java`
}

sourceSets {
    main {
        resources {
            srcDir("src/main/proto")
        }
    }
}

val zipProtoc by tasks.registering(Zip::class) {
    from("src/main/proto")
    include("**/*")
    archiveFileName.set("tachikoma-frontend-api-proto-${project.version}.zip")
    destinationDirectory.set(file("$buildDir/libs/"))
}

rootProject.extensions.configure<com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension> {
    releaseAssets.from("$buildDir/libs/tachikoma-frontend-api-proto-${project.version}.zip")
}

tasks["assemble"].dependsOn(zipProtoc)
rootProject.tasks.named("githubRelease") {
    dependsOn(zipProtoc)
}
