package graphql

import gradle.kotlin.dsl.accessors._52d8be61b0905822298db08de535ee7b.sourceSets
import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File

open class GraphqlSchemaGeneratorTask : JavaExec() {
    @get:InputDirectory
    var api = File(project.projectDir, "src/main/kotlin")

    // Fuck intellij plugin!
    @get:OutputFile
    var schemaOutput = File(project.projectDir, "schema.graphql")

    override fun configure(closure: Closure<*>): Task {
        dependsOn("classes")
        return super.configure(closure)
    }

    @TaskAction
    override fun exec() {
        val sourceSet = project.sourceSets["main"]
        classpath(sourceSet.compileClasspath + sourceSet.runtimeClasspath)
        args(schemaOutput)
        super.exec()
    }
}
