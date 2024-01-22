/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class ChangeNamespaceValueTest implements RewriteTest {

    @Test
    void replaceVersion24Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://java.sun.com/xml/ns/j2ee", "2.4", false))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="2.4"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """,
            """
              <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="2.4"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void replaceVersion25Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://java.sun.com/xml/ns/java", "2.5,3.0", false))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="2.5"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """,
            """
              <web-app xmlns="http://java.sun.com/xml/ns/java" version="2.5"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void replaceVersion30Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://java.sun.com/xml/ns/java", "2.5,3.0", false))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_0.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """,
            """
              <web-app xmlns="http://java.sun.com/xml/ns/java" version="3.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_0.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void replaceVersion31Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://xmlns.jcp.org/xml/ns/javaee", "3.1+", false))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_1.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """,
            """
              <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="3.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_1.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void replaceVersion32Test() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://xmlns.jcp.org/xml/ns/javaee", "3.1+", false))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/j2ee" version="3.2"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_2.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """,
            """
              <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="3.2"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_3_2.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void invalidVersionTest() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue("web-app", null, "http://java.sun.com/xml/ns/j2ee", "2.5", false)),
          xml(
            """
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="2.4"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
                  id="WebApp_ID">
                  <display-name>testWebDDNamespace</display-name>
              </web-app>
              """
          )
        );
    }

    @Test
    void namespaceWithPrefixMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue(null, "http://old.namespace", "https://new.namespace", null, true)),
          xml(
            """
              <ns0:parent
                  xmlns:ns0="http://old.namespace"
                  xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                      <ns0:child>value</ns0:child>
              </ns0:parent>
              """,
            """
              <ns0:parent
                  xmlns:ns0="https://new.namespace"
                  xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                      <ns0:child>value</ns0:child>
              </ns0:parent>
              """
          )
        );
    }

    @Test
    void namespaceWithoutPrefixMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue(null, "http://old.namespace", "https://new.namespace", null, true)),
          xml(
            """
              <parent
                  xmlns="http://old.namespace"
                  xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                      <child>value</child>
              </parent>
              """,
            """
              <parent
                  xmlns="https://new.namespace"
                  xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                      <child>value</child>
              </parent>
              """
          )
        );
    }

    @Test
    void namespaceNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespaceValue(null, "http://non.existant.namespace", "https://new.namespace", null, true)),
          xml(
            """
              <ns0:parent
                  xmlns:ns0="http://old.namespace"
                  xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                      <ns0:child>value</ns0:child>
              </ns0:parent>
              """
          )
        );
    }
}

