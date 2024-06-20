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
package org.openrewrite.xml.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class FindNamespacePrefixTest implements RewriteTest {

    @DocumentExample
    @Test
    void rootElement() {
        rewriteRun(
          spec -> spec.recipe(new FindNamespacePrefix("xsi", null)),
          xml(
            source,
            """
              <!--~~>--><beans xmlns="http://www.springframework.org/schema/beans"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="
                      http://cxf.apache.org/jaxws http://cxf.apache.org/schemas/jaxws.xsd
                      http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

                  <jaxws:client name="{http://cxf.apache.org/hello_world_soap_http}SoapPort" createdFromAPI="true" xmlns:jaxws="http://cxf.apache.org/jaxws">
                      <jaxws:conduitSelector>
                          <bean class="org.apache.cxf.endpoint.DeferredConduitSelector"/>
                      </jaxws:conduitSelector>
                  </jaxws:client>
              </beans>
              """
          )
        );
    }

    @Test
    void nestedElement() {
        rewriteRun(
          spec -> spec.recipe(new FindNamespacePrefix("jaxws", null)),
          xml(
            source,
            """
              <beans xmlns="http://www.springframework.org/schema/beans"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="
                      http://cxf.apache.org/jaxws http://cxf.apache.org/schemas/jaxws.xsd
                      http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

                  <!--~~>--><jaxws:client name="{http://cxf.apache.org/hello_world_soap_http}SoapPort" createdFromAPI="true" xmlns:jaxws="http://cxf.apache.org/jaxws">
                      <jaxws:conduitSelector>
                          <bean class="org.apache.cxf.endpoint.DeferredConduitSelector"/>
                      </jaxws:conduitSelector>
                  </jaxws:client>
              </beans>
              """
          )
        );
    }

    @Test
    void noMatchOnNamespacePrefix() {
        rewriteRun(
          spec -> spec.recipe(new FindNamespacePrefix("foo", null)),
          xml(source)
        );
    }

    @Test
    void noMatchOnXPath() {
        rewriteRun(
          spec -> spec.recipe(new FindNamespacePrefix("xsi", "/jaxws:client")),
          xml(source)
        );
    }

    @Test
    void staticFind() {
        rewriteRun(
          xml(
            source,
            spec -> spec.beforeRecipe(xml -> assertThat(FindNamespacePrefix.find(xml, "xsi", null))
              .isNotEmpty()
              .hasSize(1)
              .hasOnlyElementsOfType(Xml.Tag.class)
            )
          )
        );
    }

    @Language("xml")
    private final String source = """
      <beans xmlns="http://www.springframework.org/schema/beans"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="
              http://cxf.apache.org/jaxws http://cxf.apache.org/schemas/jaxws.xsd
              http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

          <jaxws:client name="{http://cxf.apache.org/hello_world_soap_http}SoapPort" createdFromAPI="true" xmlns:jaxws="http://cxf.apache.org/jaxws">
              <jaxws:conduitSelector>
                  <bean class="org.apache.cxf.endpoint.DeferredConduitSelector"/>
              </jaxws:conduitSelector>
          </jaxws:client>
      </beans>
      """;
}
