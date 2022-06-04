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
@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER", "unused")

package org.openrewrite

import org.openrewrite.groovy.tree.G
import org.openrewrite.hcl.tree.Hcl
import org.openrewrite.java.tree.J
import org.openrewrite.json.tree.Json
import org.openrewrite.properties.tree.Properties
import org.openrewrite.protobuf.tree.Proto
import org.openrewrite.quark.Quark
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpec
import org.openrewrite.test.SourceSpecs
import org.openrewrite.text.PlainText
import org.openrewrite.xml.tree.Xml
import org.openrewrite.yaml.tree.Yaml
import java.util.function.Consumer

/**
 * Kotlin version of RewriteTest that adds extension methods to RewriteTest and SourceSpecs that allow named
 * parameters to be used.
 */
interface KotlinRewriteTest : RewriteTest {

    fun RewriteTest.rewriteRun(specChange: Consumer<RecipeSpec>, vararg sourceSpecs: SourceSpecs?) {
        super.rewriteRun(specChange, *sourceSpecs)
    }

    fun RewriteTest.rewriteRun(specChange: Consumer<RecipeSpec>, vararg sourceSpecs: SourceSpec<*>?) {
        super.rewriteRun(specChange, *sourceSpecs)
    }

    fun RewriteTest.rewriteRun(vararg sourceSpecs: SourceSpecs?) {
        super.rewriteRun(*sourceSpecs)
    }

    fun RewriteTest.rewriteRun(vararg sources: SourceSpec<*>?) {
        super.rewriteRun(*sources)
    }

    fun SourceSpecs.dir(dir: String, vararg sources: SourceSpecs?): SourceSpecs {
        return super.dir(dir, *sources)
    }

    fun SourceSpecs.dir(dir: String, spec: Consumer<SourceSpec<SourceFile>>, vararg sources: SourceSpecs?): SourceSpecs {
        return super.dir(dir, spec, *sources)
    }

    fun SourceSpecs.java(before: String?): SourceSpecs {
        return super.java(before)
    }

    fun SourceSpecs.java(before: String?, spec: Consumer<SourceSpec<J.CompilationUnit>>): SourceSpecs {
        return super.java(before, spec)
    }

    fun SourceSpecs.java(before: String?, after: String): SourceSpecs {
        return super.java(before, after)
    }

    fun SourceSpecs.java(before: String?, after: String, spec: Consumer<SourceSpec<J.CompilationUnit>>): SourceSpecs {
        return super.java(before, after, spec)
    }

    fun SourceSpecs.plainText(before: String?): SourceSpecs {
        return super.plainText(before)
    }

    fun SourceSpecs.plainText(before: String?, spec: Consumer<SourceSpec<PlainText>>): SourceSpecs {
        return super.plainText(before, spec)
    }

    fun SourceSpecs.plainText(before: String?, after: String): SourceSpecs {
        return super.plainText(before, after)
    }

    fun SourceSpecs.plainText(before: String?, after: String, spec: Consumer<SourceSpec<PlainText>>): SourceSpecs {
        return super.plainText(before, after, spec)
    }

    fun SourceSpecs.pomXml(before: String?): SourceSpecs {
        return super.pomXml(before)
    }

    fun SourceSpecs.pomXml(before: String?, spec: Consumer<SourceSpec<Xml.Document>>): SourceSpecs {
        return super.pomXml(before, spec)
    }

    fun SourceSpecs.pomXml(before: String?, after: String): SourceSpecs {
        return super.pomXml(before, after)
    }

    fun SourceSpecs.pomXml(before: String?, after: String, spec: Consumer<SourceSpec<Xml.Document>>): SourceSpecs {
        return super.pomXml(before, after, spec)
    }

    fun SourceSpecs.buildGradle(before: String?): SourceSpecs {
        return super.buildGradle(before)
    }

    fun SourceSpecs.buildGradle(before: String?, spec: Consumer<SourceSpec<G.CompilationUnit>>): SourceSpecs {
        return super.buildGradle(before, spec)
    }

    fun SourceSpecs.buildGradle(before: String?, after: String): SourceSpecs {
        return super.buildGradle(before, after)
    }

    fun SourceSpecs.buildGradle(
        before: String?,
        after: String,
        spec: Consumer<SourceSpec<G.CompilationUnit>>
    ): SourceSpecs {
        return super.buildGradle(before, after, spec)
    }

    fun SourceSpecs.yaml(before: String?): SourceSpecs {
        return super.yaml(before)
    }

    fun SourceSpecs.yaml(before: String?, spec: Consumer<SourceSpec<Yaml.Documents>>): SourceSpecs {
        return super.yaml(before, spec)
    }

    fun SourceSpecs.yaml(before: String?, after: String): SourceSpecs {
        return super.yaml(before, after)
    }

    fun SourceSpecs.yaml(before: String?, after: String, spec: Consumer<SourceSpec<Yaml.Documents>>): SourceSpecs {
        return super.yaml(before, after, spec)
    }

    fun SourceSpecs.properties(before: String?): SourceSpecs {
        return super.properties(before)
    }

    fun SourceSpecs.properties(before: String?, spec: Consumer<SourceSpec<PlainText>>): SourceSpecs {
        return super.properties(before, spec)
    }

    fun SourceSpecs.properties(before: String?, after: String): SourceSpecs {
        return super.properties(before, after)
    }

    fun SourceSpecs.properties(before: String?, after: String, spec: Consumer<SourceSpec<Properties.File>>): SourceSpecs {
        return super.properties(before, after, spec)
    }

    fun SourceSpecs.json(before: String?): SourceSpecs {
        return super.json(before)
    }

    fun SourceSpecs.json(before: String?, spec: Consumer<SourceSpec<Json.Document>>): SourceSpecs {
        return super.json(before, spec)
    }

    fun SourceSpecs.json(before: String?, after: String): SourceSpecs {
        return super.json(before, after)
    }

    fun SourceSpecs.json(before: String?, after: String, spec: Consumer<SourceSpec<Json.Document>>): SourceSpecs {
        return super.json(before, after, spec)
    }

    fun SourceSpecs.proto(before: String?): SourceSpecs {
        return super.proto(before)
    }

    fun SourceSpecs.proto(before: String?, spec: Consumer<SourceSpec<Proto.Document>>): SourceSpecs {
        return super.proto(before, spec)
    }

    fun SourceSpecs.proto(before: String?, after: String): SourceSpecs {
        return super.proto(before, after)
    }

    fun SourceSpecs.proto(before: String?, after: String, spec: Consumer<SourceSpec<Proto.Document>>): SourceSpecs {
        return super.proto(before, after, spec)
    }

    fun SourceSpecs.groovy(before: String?): SourceSpecs {
        return super.groovy(before)
    }

    fun SourceSpecs.groovy(before: String?, spec: Consumer<SourceSpec<G.CompilationUnit>>): SourceSpecs {
        return super.groovy(before, spec)
    }

    fun SourceSpecs.groovy(before: String?, after: String): SourceSpecs {
        return super.groovy(before, after)
    }

    fun SourceSpecs.groovy(before: String?, after: String, spec: Consumer<SourceSpec<G.CompilationUnit>>): SourceSpecs {
        return super.groovy(before, after, spec)
    }

    fun SourceSpecs.hcl(before: String?): SourceSpecs {
        return super.hcl(before)
    }

    fun SourceSpecs.hcl(before: String?, spec: Consumer<SourceSpec<Hcl.ConfigFile>>): SourceSpecs {
        return super.hcl(before, spec)
    }

    fun SourceSpecs.hcl(before: String?, after: String): SourceSpecs {
        return super.hcl(before, after)
    }

    fun SourceSpecs.hcl(before: String?, after: String, spec: Consumer<SourceSpec<Hcl.ConfigFile>>): SourceSpecs {
        return super.hcl(before, after, spec)
    }

    fun SourceSpecs.other(before: String?): SourceSpecs {
        return super.other(before)
    }

    fun SourceSpecs.other(before: String?, spec: Consumer<SourceSpec<Quark>>): SourceSpecs {
        return super.other(before, spec)
    }

    fun SourceSpecs.text(before: String?): SourceSpecs {
        return super.text(before)
    }

    fun SourceSpecs.text(before: String?, spec: Consumer<SourceSpec<PlainText>>): SourceSpecs {
        return super.text(before, spec)
    }

    fun SourceSpecs.text(before: String?, after: String): SourceSpecs {
        return super.text(before, after)
    }

    fun SourceSpecs.text(before: String?, after: String, spec: Consumer<SourceSpec<PlainText>>): SourceSpecs {
        return super.text(before, after, spec)
    }

    fun SourceSpecs.mavenProject(
        project: String,
        spec: Consumer<SourceSpec<SourceFile>>,
        vararg sources: SourceSpecs?
    ): SourceSpecs {
        return super.mavenProject(project, spec, *sources)
    }

    fun SourceSpecs.mavenProject(project: String, vararg sources: SourceSpecs?): SourceSpecs {
        return super.mavenProject(project, *sources)
    }

    fun SourceSpecs.srcMainJava(spec: Consumer<SourceSpec<SourceFile>>, vararg javaSources: SourceSpecs?): SourceSpecs {
        return super.srcMainJava(spec, *javaSources)
    }

    fun SourceSpecs.srcMainJava(vararg javaSources: SourceSpecs?): SourceSpecs {
        return super.srcMainJava(*javaSources)
    }

    fun SourceSpecs.srcMainResources(spec: Consumer<SourceSpec<SourceFile>>, vararg resources: SourceSpecs?): SourceSpecs {
        return super.srcMainResources(spec, *resources)
    }

    fun SourceSpecs.srcMainResources(vararg resources: SourceSpecs?): SourceSpecs {
        return super.srcMainResources(*resources)
    }

    fun SourceSpecs.srcTestJava(spec: Consumer<SourceSpec<SourceFile>>, vararg javaSources: SourceSpecs?): SourceSpecs {
        return super.srcTestJava(spec, *javaSources)
    }

    fun SourceSpecs.srcTestJava(vararg javaSources: SourceSpecs?): SourceSpecs {
        return super.srcTestJava(*javaSources)
    }

    fun SourceSpecs.srcTestResources(spec: Consumer<SourceSpec<SourceFile>>, vararg resources: SourceSpecs?): SourceSpecs {
        return super.srcTestResources(spec, *resources)
    }

    fun SourceSpecs.srcTestResources(vararg resources: SourceSpecs?): SourceSpecs {
        return super.srcTestResources(*resources)
    }

}