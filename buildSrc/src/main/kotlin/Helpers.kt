import co.riiid.gradle.GithubExtension
import com.sourceforgery.tachikoma.buildsrc.DuplicateClassesExtension
import com.sourceforgery.tachikoma.buildsrc.dockerSetup
import com.sourceforgery.tachikoma.buildsrc.grpcSetup
import com.sourceforgery.tachikoma.buildsrc.javaSetup
import com.sourceforgery.tachikoma.buildsrc.kotlinSetup
import com.sourceforgery.tachikoma.buildsrc.releaseSetup
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.Cast
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.delegateClosureOf
import se.transmode.gradle.plugins.docker.DockerTask
import java.io.File
import java.util.TreeSet

fun Project.duplicateClassesChecker(configure: DuplicateClassesExtension.() -> Unit) {
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("duplicateClassesChecker", configure)
}

fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency =
    add("testImplementation", dependencyNotation)!!

fun DependencyHandler.testImplementation(dependencyNotation: Any, configureClosure: ModuleDependency.() -> Unit): Dependency =
    add("testImplementation", dependencyNotation, closureOf(configureClosure))

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency =
    add("implementation", dependencyNotation)!!

fun DependencyHandler.implementation(dependencyNotation: Any, configureClosure: ModuleDependency.() -> Unit): Dependency =
    add("implementation", dependencyNotation, closureOf(configureClosure))

fun DependencyHandler.api(dependencyNotation: Any): Dependency =
    add("api", dependencyNotation)!!

fun DependencyHandler.api(dependencyNotation: Any, configureClosure: ModuleDependency.() -> Unit): Dependency =
    add("api", dependencyNotation, closureOf(configureClosure))

val Project.sourceSets: org.gradle.api.tasks.SourceSetContainer
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer

val googleNativePrefix = OperatingSystem.current().nativePrefix
    .replace("amd64", "x86_64")

fun GithubExtension.addAssets(vararg assetsList: String) {
    val newAssets = assetsList.toCollection(TreeSet())
    val oldAssets = assets?.toSet()
        ?: emptySet()
    newAssets += oldAssets
    setAssets(*newAssets.toTypedArray())
}

fun Project.publishing(configure: org.gradle.api.publish.PublishingExtension.() -> Unit): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("publishing", configure)

/** This also handles the groovy 'with' function in a hacky manner */
fun <T> Any.delegateClosureOfHack(action: T.() -> Unit) =
    object : Closure<Unit>(this, this) {
        @Suppress("unused") // to be called dynamically by Groovy
        fun doCall() = Cast.uncheckedCast<T>(delegate)!!.action()

        @Suppress("unused") // to be called dynamically by Groovy
        fun doCall(@Suppress("UNUSED_PARAMETER") any: Any?) {
            val field = Closure::class.java.getDeclaredField("delegate")
            field.isAccessible = true
            Cast.uncheckedCast<T>(field.get(delegate))!!.action()
        }
    }

fun Project.toTree(it: File): FileTree {
    return when {
        it.isDirectory -> fileTree(it)
        it.name.endsWith("tar.gz") ||
            it.name.endsWith("tar") -> tarTree(it)
//        it.name.endsWith("jar") ||
            it.name.endsWith("zip") -> zipTree(it)
        else -> TODO("Don't know how to get a tree from $it")
    }
}

fun Task.recurseTasks(): Sequence<Task> = sequence {
    suspend fun SequenceScope<Task>.recurse(t: Task) {
        yield(t)
        val tasks = t.dependsOn
            .map {
                if (it is TaskProvider<*>) {
                    it.get()
                } else {
                    it
                }
            }
            .filterIsInstance<Task>()
        for (it in tasks) {
            recurse(it)
        }
    }
    recurse(this@recurseTasks)
}

fun Project.applyJava() = javaSetup()
fun Project.applyKotlin() = kotlinSetup()
fun Project.applyRelease() = releaseSetup()
fun Project.applyGrpc() = grpcSetup()
fun Project.applyDocker() = dockerSetup()
