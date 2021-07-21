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
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.tree.Yaml

class JsonPathMatcherTest {

    val json = """
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
    val simpleJsonPath = "$.metadata"
    val appLabel = "$.metadata.labels.app"
    val recurseSpecContainers = "..spec.containers"
    val firstContainerSlice = "$.spec.template.spec.containers[:1]"
    val containerByNameImage = "..spec.containers[?(@.name == 'app')].image"

    @Test
    fun `must find expression result`() {
        val results = visit(appLabel, json)
        assertThat(results).hasSize(2)
        assertThat(results.get(0) is Yaml.Mapping.Entry).isTrue()
        assertThat((((results.get(0) as Yaml.Mapping.Entry).value) as Yaml.Scalar).value).isEqualTo("myapp")
    }

    @Test
    fun `must recurse to find elements`() {
        val results = visit(recurseSpecContainers, json)
        assertThat(results).hasSize(3)
    }

    @Test
    fun `must find enclosed elements`() {
        val results = visit(recurseSpecContainers, json, true)
        assertThat(results).hasSize(6)
    }

    @Test
    fun `must slice sequences`() {
        val results = visit(firstContainerSlice, json)
        assertThat(results).hasSize(2)
    }

    @Test
    fun `must filter by expression`() {
        val results = visit(containerByNameImage, json)
        assertThat(results).hasSize(1)
        assertThat(((results.get(0) as Yaml.Mapping.Entry).value as Yaml.Scalar).value).isEqualTo("mycompany.io/app:v2@digest")
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
                    val matches = if (encloses) matcher.encloses(cursor) else matcher.matches(cursor)
                    if (matches) {
                        val e = super.visitMappingEntry(entry, p)
                        p.add(e)
                        return e
                    }
                    return super.visitMappingEntry(entry, p)
                }
            }.visit(it, results)
        }
        return results
    }

}