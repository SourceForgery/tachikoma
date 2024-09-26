import com.sourceforgery.tachikoma.buildsrc.CheckDuplicateClassesTask
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    `kotlin`
    id("org.jlleitschuh.gradle.ktlint")
    id("tachikoma-dependencies")
}

val javaVersion: String by project
val kotlinVersion: String by project
val unshadowedKotlinVersion = kotlinVersion

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
    compilerOptions {
        val kotlinApiVersion = KotlinVersion.fromVersion(unshadowedKotlinVersion.substringBeforeLast('.'))
        languageVersion.set(kotlinApiVersion)
        apiVersion = kotlinApiVersion
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
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
