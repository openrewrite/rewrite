/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.java.marker.JavaProject
import org.openrewrite.marker.Marker
import org.openrewrite.maven.MavenParser
import org.openrewrite.maven.tree.MavenResolutionResult
import org.openrewrite.maven.tree.ResolvedPom
import org.openrewrite.xml.tree.Xml


interface MavenTestingSupport : XmlTestingSupport {

    /**
     * Extension method for {@Link MavenParser} that will parse a maven file and add a new JavaProject provenance to the
     * resulting source file. An optional list of additional markers can be associated with the source file.
     */
    fun MavenParser.parseMavenProjects(
        @Language("xml") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): Xml.Document {
        return parse(ctx, source.trimIndent())[0].let {
            it.addMarkers(listOf(createJavaProjectFromMaven(it)) + markers)
        }
    }

    /**
     * Extension method for {@Link MavenParser} that will parse a maven file and add a new JavaProject provenance to each
     * resulting source file. An optional list of additional markers can be associated with each source file.
     */
    fun MavenParser.parseMavenProjects(
        @Language("xml") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<Xml.Document> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(listOf(createJavaProjectFromMaven(it)) + markers)
        }
    }

    fun Xml.Document.getPom() : ResolvedPom {
        return this.getModel().pom
    }

    fun Xml.Document.getModel() : MavenResolutionResult {
        return markers.findFirst(MavenResolutionResult::class.java)
            .orElseThrow { IllegalStateException("The XML Document is not a maven source file.") }
    }

    fun Xml.Document.getJavaProject() : JavaProject {
        return markers.findFirst(JavaProject::class.java)
            .orElseThrow { IllegalStateException("There is no Java Project associated with the document") }
    }

    private fun createJavaProjectFromMaven(maven : Xml.Document) : JavaProject {
        val pom = maven.getPom()
        return JavaProject(Tree.randomId(), pom.artifactId, JavaProject.Publication(pom.groupId, pom.artifactId, pom.version))
    }
}