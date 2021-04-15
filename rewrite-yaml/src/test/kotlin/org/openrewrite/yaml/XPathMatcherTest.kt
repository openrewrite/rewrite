/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.yaml.tree.Yaml

class XPathMatcherTest {
    private val y = YamlParser().parse(
        """
            apiVersion: v1
            kind: ServiceAccount
            metadata:
              another:
                name: value
              name: monitoring-tools
              namespace: monitoring-tools
            ---
            apiVersion: rbac.authorization.k8s.io/v1beta1
            kind: ClusterRoleBinding
            metadata:
              name: monitoring-tools
            subjects:
              - kind: ServiceAccount
                name: monitoring-tools
                namespace: monitoring-tools
        """.trimIndent()
    ).iterator().next()

    private fun visit(xpath: String): Boolean {
        val matchElements = mutableListOf<Yaml>()
        visitor(xpath).visit(y, matchElements)
        return matchElements.isNotEmpty()
    }

    @Test
    fun matchSequences() {
        assertThat(visit("/subjects/kind")).isTrue
    }

    @Test
    fun matchAbsolute() {
        assertThat(visit("/apiVersion")).isTrue
        assertThat(visit("/metadata/*")).isTrue
        assertThat(visit("/metadata/dne")).isFalse
    }

    @Test
    fun matchRelative() {
        assertThat(visit("apiVersion")).isTrue
        assertThat(visit("namespace")).isTrue
        assertThat(visit("//namespace")).isTrue
        assertThat(visit("another/*")).isTrue
        assertThat(visit("dne")).isFalse
    }

    private fun visitor(xPath: String): YamlVisitor<MutableList<Yaml>> {
        val matcher = XPathMatcher(xPath)

        return object : YamlVisitor<MutableList<Yaml>>() {
            override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: MutableList<Yaml>): Yaml {
                if(matcher.matches(cursor)) {
                    p.add(entry)
                }
                return super.visitMappingEntry(entry, p)
            }
        }
    }
}
