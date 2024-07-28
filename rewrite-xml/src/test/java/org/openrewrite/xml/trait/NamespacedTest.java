/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.trait;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class NamespacedTest {

    @Test
    void isNamespaceDefinitionAttributeTests() {
        assertThat(Namespaced.isNamespaceDefinitionAttribute("xmlns:test")).isTrue();
        assertThat(Namespaced.isNamespaceDefinitionAttribute("test")).isFalse();
    }

    @Test
    void getAttributeNameForPrefix() {
        assertThat(Namespaced.getAttributeNameForPrefix("test")).isEqualTo("xmlns:test");
        assertThat(Namespaced.getAttributeNameForPrefix("")).isEqualTo("xmlns");
    }

    @Test
    void extractNamespacePrefix() {
        assertEquals("test", Namespaced.extractNamespacePrefix("test:tag"));
        assertEquals("", Namespaced.extractNamespacePrefix("tag"));
    }

    @Test
    void extractLocalName() {
        assertEquals("tag", Namespaced.extractLocalName("test:tag"));
        assertEquals("tag", Namespaced.extractLocalName("tag"));
    }

    @Test
    void extractPrefixFromNamespaceDefinition() {
        assertEquals("test", Namespaced.extractPrefixFromNamespaceDefinition("xmlns:test"));
        assertEquals("", Namespaced.extractPrefixFromNamespaceDefinition("xmlns"));
        assertThat(Namespaced.extractPrefixFromNamespaceDefinition("test")).isEqualTo(null);
        assertThat(Namespaced.extractPrefixFromNamespaceDefinition("a:test")).isEqualTo(null);
    }
}
