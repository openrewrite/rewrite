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
package org.openrewrite.xml.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class XmlTest {

    @Test
    void isNamespaceDefinitionAttributeTests() {
        assertThat(Xml.isNamespaceDefinitionAttribute("xmlns:test")).isTrue();
        assertThat(Xml.isNamespaceDefinitionAttribute("test")).isFalse();
    }

    @Test
    void getAttributeNameForPrefix() {
        assertThat(Xml.getAttributeNameForPrefix("test")).isEqualTo("xmlns:test");
        assertThat(Xml.getAttributeNameForPrefix("")).isEqualTo("xmlns");
    }

    @Test
    void extractNamespacePrefix() {
        assertEquals("test", Xml.extractNamespacePrefix("test:tag"));
        assertEquals("", Xml.extractNamespacePrefix("tag"));
    }

    @Test
    void extractLocalName() {
        assertEquals("tag", Xml.extractLocalName("test:tag"));
        assertEquals("tag", Xml.extractLocalName("tag"));
    }

    @Test
    void extractPrefixFromNamespaceDefinition() {
        assertEquals("test", Xml.extractPrefixFromNamespaceDefinition("xmlns:test"));
        assertEquals("", Xml.extractPrefixFromNamespaceDefinition("xmlns"));
        assertThat(Xml.extractPrefixFromNamespaceDefinition("test")).isEqualTo(null);
        assertThat(Xml.extractPrefixFromNamespaceDefinition("a:test")).isEqualTo(null);
    }
}
