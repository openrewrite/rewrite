/*
 * Copyright 2026 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.xml.Assertions.xml;

class WebXmlReferenceTest implements RewriteTest {

    @Test
    void prefixedWebFragmentReferences() {
        rewriteRun(xml(
          """
            <j:web-fragment xmlns:j="https://jakarta.ee/xml/ns/jakartaee">
              <j:error-page><j:exception-type>example.Failure</j:exception-type></j:error-page>
              <j:filter><j:exception-type>example.Ignored</j:exception-type></j:filter>
            </j:web-fragment>
            """,
          spec -> spec.path("META-INF/web-fragment.xml").beforeRecipe(doc ->
            assertThat(doc.getReferences().getReferences()).extracting(Reference::getKind, Reference::getValue)
              .containsExactly(tuple(Reference.Kind.TYPE, "example.Failure")))
        ));
    }

    @Test
    void servletClassNames() {
        rewriteRun(xml(
          """
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee">
              <servlet><servlet-class>defaultServlet</servlet-class></servlet>
              <filter><filter-class>acme.lowercase</filter-class></filter>
              <listener><listener-class>Listener</listener-class></listener>
              <error-page><exception-type>com.acme.Outer$Nested</exception-type></error-page>
              <listener><listener-class>acme.123</listener-class></listener>
              <filter><filter-class>acme.class</filter-class></filter>
            </web-app>
            """,
          spec -> spec.path("WEB-INF/web.xml").beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getKind, Reference::getValue)
            .containsExactlyInAnyOrder(tuple(Reference.Kind.TYPE, "defaultServlet"),
              tuple(Reference.Kind.TYPE, "acme.lowercase"), tuple(Reference.Kind.TYPE, "Listener"),
              tuple(Reference.Kind.TYPE, "com.acme.Outer$Nested")))
        ));
    }

    @ParameterizedTest
    @CsvSource({
      "web-app, http://java.sun.com/xml/ns/j2ee",
      "web-app, http://java.sun.com/xml/ns/javaee",
      "web-fragment, http://xmlns.jcp.org/xml/ns/javaee",
      "web-fragment, https://jakarta.ee/xml/ns/jakartaee"
    })
    void customDescriptorPaths(String root, String namespace) {
        String descriptor = """
          <%1$s xmlns="%2$s"><filter><filter-class>acme.Filter</filter-class></filter></%1$s>
          """.formatted(root, namespace);
        rewriteRun(xml(descriptor, spec -> spec.path("jetty/webdefault.xml").beforeRecipe(doc ->
          assertThat(doc.getReferences().getReferences()).extracting(Reference::getValue)
            .containsExactly("acme.Filter"))));
    }

    @Test
    void dtdDescriptor() {
        rewriteRun(xml(
          """
            <!DOCTYPE web-app PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
                "http://java.sun.com/dtd/web-app_2_3.dtd">
            <web-app><listener><listener-class>example.Listener</listener-class></listener></web-app>
            """,
          spec -> spec.path("override-web.xml").beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getValue).containsExactly("example.Listener"))
        ));
    }

    @Test
    void unrelatedNamespaceOrFileExtension() {
        String tag = "<servlet><servlet-class>example.Other</servlet-class></servlet>";
        rewriteRun(
          xml("<web-app xmlns='urn:other'>" + tag + "</web-app>",
            spec -> spec.path("web.xml").beforeRecipe(doc ->
              assertThat(doc.getReferences().getReferences()).isEmpty())),
          xml("<web-app xmlns='https://jakarta.ee/xml/ns/jakartaee'>" + tag + "</web-app>",
            spec -> spec.path("web.txt").beforeRecipe(doc ->
              assertThat(doc.getReferences().getReferences()).isEmpty()))
        );
    }
}
