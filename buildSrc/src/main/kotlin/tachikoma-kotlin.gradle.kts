import com.sourceforgery.tachikoma.buildsrc.CheckDuplicateClassesTask
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    `kotlin`
    id("org.jlleitschuh.gradle.ktlint")
    id("tachikoma-dependencies")
}

val javaVersion: String by project
val kotlinVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}


dependencies {
    for (forcedBom in forcedBoms()) {
        implementation(enforcedPlatform(forcedBom))
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        val kotlinApiVersion = kotlinVersion.substringBeforeLast('.')
        languageVersion = kotlinApiVersion
        apiVersion = kotlinApiVersion
        jvmTarget = javaVersion
        freeCompilerArgs = listOf(
            "-java-parameters",
            "-Xjsr305=strict",
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
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
