/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.yaml

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.yaml.tree.Yaml

class JsonPathMatcherTest {
    @Language("yaml")
    private val source = """
        apiVersion: v1
        kind: Pod
        metadata:
          name: barepodA
          labels:
            app: myapp
            role: A
          annotations:
            mycompany.io/commit-hash: hash
        spec:
          containers:
            - name: nginx
              image: nginx:latest
        ---
        apiVersion: v1
        kind: Pod
        metadata:
          name: barepodB
          labels:
            app: myapp
            role: B
          annotations:
            mycompany.io/commit-hash: hash
        spec:
          containers:
            - name: nginx
              image: nginx:latest
        ---
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          namespace: myns
          name: app-deployment
        spec:
          template:
            spec:
              metadata:
                annotations:
                  mycompany.io/commit-hash: hash
              containers:            
                - name: app
                  image: mycompany.io/app:v2@digest
              initContainers:
                - name: init
                  image: mycompany.io/init:latest
    """.trimIndent()

    private val topLevelKeys = "$.*"
    private val appLabel = "$.metadata.labels.app"
    private val appLabelBracket = "$.metadata.labels['app']"
    private val recurseSpecContainers = "..spec.containers"
    private val firstContainerSlice = "$.spec.template.spec.containers[:1]"
    private val allContainerSlices = "$.spec.template.spec.containers[*]"
    private val allSpecChildren = "$.spec.template.spec.*"
    private val containerByNameImage = "..spec.containers[?(@.name == 'app')].image"
    private val containerByNameImageMatches = "..spec.containers[?(@.name =~ 'a.*')].image"
    private val containerByNameImageWithAnd = "..spec.containers[?(@.name =~ 'a.*' && @.image =~ 'mycompany.*')].image"
    private val image = ".image"

    @Test
    fun `must identify top-level elements`() {
        val results = visit(topLevelKeys, source)
        assertThat(results).hasSize(12)
    }

    @Test
    fun `must find expression result`() {
        val results = visit(appLabel, source)
        assertThat(results).hasSize(2)
        assertThat(results[0] is Yaml.Mapping.Entry).isTrue
        assertThat((((results[0] as Yaml.Mapping.Entry).value) as Yaml.Scalar).value).isEqualTo("myapp")
    }

    @Test
    fun `must recurse to find elements`() {
        val results = visit(recurseSpecContainers, source)
        assertThat(results).hasSize(3)
    }

    @Test
    fun `must find enclosed elements`() {
        val results = visit(recurseSpecContainers, source, true)
        assertThat(results).hasSize(6)
    }

    @Test
    fun `must slice sequences`() {
        val results = visit(firstContainerSlice, source, true)
        assertThat(results).hasSize(2)
    }

    @Test
    fun `must filter by expression`() {
        val results = visit(containerByNameImage, source)
        assertThat(results).hasSize(1)
        assertThat(((results[0] as Yaml.Mapping.Entry).value as Yaml.Scalar).value).isEqualTo("mycompany.io/app:v2@digest")
    }

    @Test
    fun `must filter by pattern`() {
        val results = visit(containerByNameImageMatches, source)
        assertThat(results).hasSize(1)
        assertThat(((results[0] as Yaml.Mapping.Entry).value as Yaml.Scalar).value).isEqualTo("mycompany.io/app:v2@digest")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1063")
    @Disabled
    @Test
    fun `must filter by filters combined with and`() {
        val results = visit(containerByNameImageWithAnd, source)
        assertThat(results).hasSize(1)
        assertThat(((results[0] as Yaml.Mapping.Entry).value as Yaml.Scalar).value).isEqualTo("mycompany.io/app:v2@digest")
    }

    @Test
    fun `must filter by relative expression`() {
        val results = visit(image, source)
        assertThat(results).hasSize(4)
    }

    @Test
    fun `must support wildcards as identifiers`() {
        val results = visit(allSpecChildren, source)
        assertThat(results).hasSize(3)
    }

    @Test
    fun `must support wildcards in range operators`() {
        val results = visit(allContainerSlices, source, true)
        assertThat(results).hasSize(2)
    }

    @Test
    fun `must support identifiers in bracket operators`() {
        val results = visit(appLabelBracket, source)
        assertThat(results).hasSize(2)
        assertThat(results[0] is Yaml.Mapping.Entry).isTrue
        assertThat((((results[0] as Yaml.Mapping.Entry).value) as Yaml.Scalar).value).isEqualTo("myapp")
    }

    @Test
    fun `must find mapping at document level`() {
        val results = visitDocument(appLabel, source)
        @Suppress("SameParameterValue")
        assertThat(results).hasSize(2)
    }

    private fun visit(jsonPath: String, json: String, encloses: Boolean = false): List<Yaml> {
        val ctx = InMemoryExecutionContext({ it.printStackTrace() })
        val documents = YamlParser().parse(ctx, json)
        if (documents.isEmpty()) {
            return emptyList()
        }
        val matcher = JsonPathMatcher(jsonPath)

        val results = ArrayList<Yaml>()
        documents.forEach {
            object : YamlVisitor<MutableList<Yaml>>() {
                override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: MutableList<Yaml>): Yaml? {
                    val e = super.visitMappingEntry(entry, p)
                    if (if (encloses) matcher.encloses(cursor) else matcher.matches(cursor)) {
                        p.add(e)
                    }
                    return e
                }
            }.visit(it, results)
        }
        return results
    }

    private fun visitDocument(jsonPath: String, json: String): List<Yaml> {
        val ctx = InMemoryExecutionContext { it.printStackTrace() }
        val documents = YamlParser().parse(ctx, json)
        if (documents.isEmpty()) {
            return emptyList()
        }
        val matcher = JsonPathMatcher(jsonPath)

        val results = ArrayList<Yaml>()
        documents.forEach {
            object : YamlVisitor<MutableList<Yaml>>() {
                override fun visitDocument(document: Yaml.Document, p: MutableList<Yaml>): Yaml {
                    val d = super.visitDocument(document, p)
                    if (matcher.find<Yaml>(cursor).isPresent) {
                        p.add(d)
                    }
                    return d
                }
            }.visit(it, results)
        }
        return results
    }
}
