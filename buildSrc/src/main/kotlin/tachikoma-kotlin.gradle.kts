import com.sourceforgery.tachikoma.buildsrc.CheckDuplicateClassesTask
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    `kotlin`
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutineVersion"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.junit.platform:junit-platform-runner:1.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.6"
        apiVersion = "1.6"
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-java-parameters",
            "-Xjsr305=strict",
            "-Xjvm-default=enable",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

extensions.configure<KtlintExtension> {
    debug.set(false)
    verbose.set(true)
    android.set(false)
    ignoreFailures.set(false)
    filter {
        exclude { "/generated/" in it.file.path }
    }
    disabledRules.set(listOf("final-newline"))
    version.set("0.44.0")
}

val sourceJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allJava)
    archiveClassifier.set("source")
}
tasks.assemble {
    dependsOn(sourceJar)
}

val checkDuplicateClasses by tasks.registering(CheckDuplicateClassesTask::class)

tasks.check {
    dependsOn(checkDuplicateClasses)
}