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

import static org.junit.jupiter.api.Assertions.*;

class XmlNamespaceUtilsTest {

    @Test
    void isNamespaceDefinitionAttributeTests() {
        assertTrue(XmlNamespaceUtils.isNamespaceDefinitionAttribute("xmlns:test"));
        assertFalse(XmlNamespaceUtils.isNamespaceDefinitionAttribute("test"));
    }

    @Test
    void getAttributeNameForPrefix() {
        assertEquals("xmlns:test", XmlNamespaceUtils.getAttributeNameForPrefix("test"));
        assertEquals("xmlns", XmlNamespaceUtils.getAttributeNameForPrefix(""));
    }

    @Test
    void extractNamespacePrefix() {
        assertEquals("test", XmlNamespaceUtils.extractNamespacePrefix("test:tag"));
        assertEquals("", XmlNamespaceUtils.extractNamespacePrefix("tag"));
    }

    @Test
    void extractLocalName() {
        assertEquals("tag", XmlNamespaceUtils.extractLocalName("test:tag"));
        assertEquals("tag", XmlNamespaceUtils.extractLocalName("tag"));
    }

    @Test
    void extractPrefixFromNamespaceDefinition() {
        assertEquals("test", XmlNamespaceUtils.extractPrefixFromNamespaceDefinition("xmlns:test"));
        assertEquals("", XmlNamespaceUtils.extractPrefixFromNamespaceDefinition("xmlns"));
        assertThrows(IllegalArgumentException.class, () -> XmlNamespaceUtils.extractPrefixFromNamespaceDefinition("test"));
        assertThrows(IllegalArgumentException.class, () -> XmlNamespaceUtils.extractPrefixFromNamespaceDefinition("a:test"));
    }
}