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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NamespacedTest {

    @Test
    void defaultNamespaceAppliesToUnprefixedElementsOnly() {
        String xml = """
          <root xmlns="urn:default" id="1"/>
          """;
        assertThat(namespaced(xml, "root").getNamespaceUri()).hasValue("urn:default");

        assertThat(namespaced(xml, "id").getNamespaceUri()).isEmpty();
    }

    @Test
    void emptyDefaultNamespaceUndeclaresIt() {
        Namespaced child = namespaced(
          """
            <root xmlns="urn:default">
                <child xmlns=""/>
            </root>
            """,
          "child"
        );
        assertThat(child.getNamespaceUri()).isEmpty();
    }

    @Test
    void innerDeclarationShadowsOuter() {
        Namespaced inner = namespaced(
          """
            <p:root xmlns:p="urn:outer">
                <p:inner xmlns:p="urn:inner"/>
            </p:root>
            """,
          "p:inner"
        );
        assertThat(inner.getAllNamespaces()).containsEntry("p", "urn:inner");
    }

    @Test
    void plainAttributeDeclaresNoNamespace() {
        assertThat(namespaced("<root id=\"1\"/>", "id").getNamespaces()).isEmpty();
    }

    @Test
    void reservedNamespacesNeedNoDeclaration() {
        String xml = """
          <root xml:lang="en" xmlns="urn:default" xmlns:p="urn:p"/>
          """;
        assertThat(namespaced(xml, "xml:lang").getNamespaceUri()).hasValue("http://www.w3.org/XML/1998/namespace");

        assertThat(namespaced(xml, "xmlns").getNamespaceUri()).hasValue("http://www.w3.org/2000/xmlns/");

        assertThat(namespaced(xml, "xmlns:p").getNamespaceUri()).hasValue("http://www.w3.org/2000/xmlns/");
    }

    @Test
    void schemaLocationsResolveEachAttributesOwnNamespace() {
        Namespaced root = namespaced(
          """
            <root xmlns="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  schemaLocation="urn:a a.xsd"
                  xsi:schemaLocation="urn:b b.xsd"/>
            """,
          "root"
        );
        assertThat(root.getSchemaLocations())
          .extracting(Xml.Attribute::getKeyAsString)
          .containsExactly("xsi:schemaLocation");
    }

    @Test
    void schemaLocationsAreTheLocationHintsOnly() {
        Namespaced root = namespaced(
          """
            <root xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="urn:a a.xsd"
                  xsi:noNamespaceSchemaLocation="b.xsd"
                  xsi:nil="true"/>
            """,
          "root"
        );
        assertThat(root.getSchemaLocations())
          .extracting(Xml.Attribute::getKeyAsString)
          .containsExactly("xsi:schemaLocation", "xsi:noNamespaceSchemaLocation");
    }

    private static Namespaced namespaced(@Language("xml") String xml, String name) {
        return Namespaced.matcher()
          .lower(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), new XmlParser().parse(xml).findFirst().orElseThrow()))
          .filter(n -> n.getName().filter(name::equals).isPresent())
          .findFirst()
          .orElseThrow();
    }

    @Test
    void isNamespaceDefinitionAttributeTests() {
        assertThat(Namespaced.isNamespaceDefinitionAttribute("xmlns:test")).isTrue();
        assertThat(Namespaced.isNamespaceDefinitionAttribute("test")).isFalse();
        assertThat(Namespaced.isNamespaceDefinitionAttribute("xmlnsVersion")).isFalse();
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
