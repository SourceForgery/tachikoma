package maven

import com.google.common.cache.CacheBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.kotlin.dsl.DependencyConstraintHandlerScope
import org.gradle.kotlin.dsl.getArtifacts
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

fun DependencyConstraintHandler.api(constraintNotation: Any): DependencyConstraint =
    add("api", constraintNotation)

fun DependencyConstraintHandlerScope.unrollBom(project: Project, dependency: String) {
    val (groupId, artifactId, version) = dependency.split(":")
    unrollBom(project, groupId, artifactId, version)
}

fun DependencyConstraintHandlerScope.unrollBom(project: Project, groupId: String, artifactId: String, version: String) {
    val id = ArtifactId(
        groupId = groupId,
        artifactId = artifactId,
        version = version
    )
    val bom = recursiveUnrollBom(id, project.createPomReader())

    for (dep in bom.recursiveDependencies()) {
        api(dep)
        project.logger.info("Adding constraint dep on $dep")
    }
}

fun ResolutionStrategy.unrollBomForce(project: Project, dependency: String) {
    val (groupId, artifactId, version) = dependency.split(":")
    unrollBomForce(project, groupId, artifactId, version)
}

fun ResolutionStrategy.unrollBomForce(project: Project, groupId: String, artifactId: String, version: String) {
    val id = ArtifactId(
        groupId = groupId,
        artifactId = artifactId,
        version = version
    )
    val bom = recursiveUnrollBom(id, project.createPomReader())

    force(bom.recursiveDependencies())
}

typealias PomFileReader = (ArtifactId) -> ByteArray

fun Project.createPomReader(): PomFileReader = { id ->
    val result = project.dependencies.createArtifactResolutionQuery()
        .forModule(id.groupId, id.artifactId, id.version)
        .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
        .execute()
        .resolvedComponents

    check(result.size > 0) {
        "Didn't find any resolved component for ${id.stringId}. If you wonder what a component is, so do I."
    }

    check(result.size == 1) {
        "Not exactly one (found $result) resolved component for ${id.stringId}. Code needs a loop. If you wonder what a component is, so do I."
    }
    val component = result.first()

    val mavenPomFiles = component.getArtifacts(MavenPomArtifact::class)
        .filterIsInstance<ResolvedArtifactResult>()
    mavenPomFiles
        .map { it.file }
        .first()
        .readBytes()
}

fun Map<String, String>.resolve(
    value: String,
    @Suppress("UNUSED_PARAMETER")
    name: String? = null
): String {
    @Suppress("RegExpRedundantEscape")
    val regex = """\$\{([^}]+)\}""".toRegex()
    return regex.replace(value) {
        this[it.groupValues[1]]
            ?: it.groupValues[0]
//                ?: error("Couldn't find $name prop ${it.groupValues[1]} in $properties")
    }
}

internal fun recursiveUnrollBom(
    artifactId: ArtifactId,
    pomFileReader: PomFileReader
): Pom =
    POM_CACHE.get(artifactId) {
        parsePom(
            bomFile = pomFileReader(artifactId),
            artifactId = artifactId,
            pomFileReader = pomFileReader
        )
    }

internal fun parsePom(
    bomFile: ByteArray,
    artifactId: ArtifactId,
    pomFileReader: PomFileReader
): Pom {
    val pomDom = builder.parse(ByteArrayInputStream(bomFile)).documentElement

    val parentPom = pomDom["parent"][0]?.let { parent ->
        recursiveUnrollBom(
            artifactId = ArtifactId(
                groupId = parent["groupId"][0].textContent,
                artifactId = parent["artifactId"][0].textContent,
                version = parent["version"][0].textContent
            ),
            pomFileReader = pomFileReader
        )
    }

    val properties = parentPom?.recursiveProperties?.toMutableMap()
        ?: mutableMapOf()

    properties["project.groupId"] = artifactId.groupId
    properties["project.artifactId"] = artifactId.artifactId
    properties["project.version"] = artifactId.version
    properties["project.build.directory"] = ""

    pomDom["properties"][0]?.let { props ->
        props.asSequence()
            .associateTo(properties) {
                it.nodeName to properties.resolve(it.textContent, it.nodeName)
            }
    }

    fun Node.resolveNode(attrib: String, properties: Map<String, String>) =
        properties.resolve(this[attrib][0].textContent ?: error("$this[$attrib]"), "$attrib : $this")

    val dependencies = pomDom["dependencyManagement"][0]?.let { dependencyManagement ->
        dependencyManagement["dependencies"][0]
            .asSequence()
            .filter { it.nodeName == "dependency" }
            .map { attrs: Node ->
                "${attrs.resolveNode("groupId", properties)}:${attrs.resolveNode("artifactId", properties)}:${attrs.resolveNode("version", properties)}"
            }
            .toList()
    } ?: emptyList()

    val pom = Pom(
        artifactId = artifactId,
        parentPom = parentPom,
        dependencies = dependencies,
        recursiveProperties = properties
    )
    return pom
}

// This assumes no poms will ever change during compilation.
// I *believe* this to be a correct assumption
internal val POM_CACHE = CacheBuilder.newBuilder().build<ArtifactId, Pom>()

private val factory = DocumentBuilderFactory.newInstance()
private val builder = factory.newDocumentBuilder()

data class ArtifactId(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    val stringId = "$groupId:$artifactId:$version"
}

internal data class Pom(
    val artifactId: ArtifactId,
    val parentPom: Pom?,
    val recursiveProperties: Map<String, String>,
    val dependencies: List<String>
) {
    fun recursiveDependencies(): List<String> =
        generateSequence(this) { it.parentPom }
            .toList()
            .asReversed()
            .flatMap { it.dependencies }
}

fun Node.asSequence(): Sequence<Element> {
    return if (!hasChildNodes()) {
        emptySequence()
    } else {
        generateSequence(firstChild) { child ->
            child.nextSibling
        }.filterIsInstance<Element>()
    }
}

operator fun NodeList.get(index: Int) =
    item(index)

operator fun Node.get(key: String) =
    (this as Element).getElementsByTagName(key)
