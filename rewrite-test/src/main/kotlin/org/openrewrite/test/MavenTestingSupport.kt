package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.Tree
import org.openrewrite.java.marker.JavaProject
import org.openrewrite.marker.Marker
import org.openrewrite.maven.MavenParser
import org.openrewrite.maven.tree.MavenResolutionResult
import org.openrewrite.maven.tree.ResolvedPom
import org.openrewrite.xml.tree.Xml


interface MavenTestingSupport : XmlTestingSupport {
    val mavenParser: MavenParser
        get() = MavenParser.builder().build()

    fun MavenParser.parseMavenProjects(
        @Language("xml") vararg sources: String
    ): List<Xml.Document> {
        return parse(executionContext, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(listOf(createJavaProjectFromMaven(it)))
        }
    }

    fun MavenParser.parseMavenProjects(
        markers : List<Marker> = emptyList(),
        @Language("xml") vararg sources: String
    ): List<Xml.Document> {
        return parse(executionContext, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(listOf(createJavaProjectFromMaven(it)) + markers)
        }
    }

    fun Xml.Document.getPom() : ResolvedPom {
        return this.getModel().pom
    }

    fun Xml.Document.getModel() : MavenResolutionResult {
        return markers.findFirst(MavenResolutionResult::class.java)
            .orElseThrow { IllegalStateException("The XML Document is not a maven source file.") };
    }

    fun Xml.Document.getJavaProject() : JavaProject {
        return markers.findFirst(JavaProject::class.java)
            .orElseThrow { IllegalStateException("There is no Java Project associated with the document") };
    }

    fun createJavaProjectFromMaven(maven : Xml.Document) : JavaProject {
        val model = maven.getModel()
        return JavaProject(Tree.randomId(), model.pom.artifactId, JavaProject.Publication(model.pom.groupId, model.pom.artifactId, model.pom.version))
    }
}