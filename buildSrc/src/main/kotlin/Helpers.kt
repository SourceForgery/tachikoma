import com.sourceforgery.tachikoma.buildsrc.DuplicateClassesExtension
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.Cast
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.closureOf
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.TreeSet

fun Project.duplicateClassesChecker(configure: DuplicateClassesExtension.() -> Unit) {
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("duplicateClassesChecker", configure)
}

val googleNativePrefix = OperatingSystem.current().nativePrefix
    .replace("amd64", "x86_64")

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
