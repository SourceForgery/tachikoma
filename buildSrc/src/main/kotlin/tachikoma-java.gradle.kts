import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    from(tasks.javadoc.get().destinationDir)
    archiveClassifier.set("javadoc")
}

tasks.withType<Javadoc> {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("Xdoclint:none", "-quiet")
}

val sourceJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allJava)
    archiveClassifier.set("source")
}

tasks.withType<JavaCompile> {
    options.compilerArgs = listOf("-Xlint:unchecked", "-Xlint:deprecation")
}

tasks.assemble {
    dependsOn(sourceJar)
    dependsOn(javadocJar)
}
