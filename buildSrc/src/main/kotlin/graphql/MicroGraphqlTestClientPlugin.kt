package graphql

import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.util.Path

fun Project.addGraphqlSchemaGenerator(schemaGeneratorPath: String) {
    val projectPath = Path.path(schemaGeneratorPath).parent!!
    evaluationDependsOn(projectPath.path)
    tasks.named<GraphQLGenerateClientTask>("graphqlGenerateClient") {
        val schemaGenerator = tasks.getByPath(schemaGeneratorPath)
        require(schemaGenerator is GraphqlSchemaGeneratorTask) {
            "Task $schemaGeneratorPath is not a ${GraphqlSchemaGeneratorTask::class.java}"
        }
        dependsOn(schemaGenerator)
        schemaFile.set(schemaGenerator.outputs.files.singleFile)
    }
}

fun Project.addGraphqlSchemaGenerator(schemaGenerator: GraphqlSchemaGeneratorTask) {
    tasks.named<GraphQLGenerateClientTask>("graphqlGenerateClient") {
        dependsOn(schemaGenerator)
        schemaFile.set(schemaGenerator.outputs.files.singleFile)
    }
}
