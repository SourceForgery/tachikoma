package graphql

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.DefaultSourceSetContainer
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get

@Suppress("LeakingThis")
abstract class GraphqlSchemaGeneratorTask : JavaExec() {
    @get:InputDirectory
    abstract val api: DirectoryProperty

    // Fuck intellij plugin!
    @get:OutputFile
    abstract val schemaOutput: RegularFileProperty

    init {
        api.convention(project.layout.projectDirectory.dir("src/main/kotlin"))
        schemaOutput.convention(project.layout.buildDirectory.map { it.file("schema.graphqls") })
    }
    override fun configure(closure: Closure<*>): Task {
        dependsOn("classes")
        return super.configure(closure)
    }

    @TaskAction
    override fun exec() {
        val sourceSets = project.extensions.getByName("sourceSets") as DefaultSourceSetContainer
        val sourceSet = sourceSets["main"]
        classpath(sourceSet.compileClasspath + sourceSet.runtimeClasspath)
        args(schemaOutput.get())
        super.exec()
    }
}
