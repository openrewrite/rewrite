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
package org.openrewrite.xml;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openrewrite.xml.Assertions.xml;

class XsltTransformationTest implements RewriteTest {

    @Language("xml")
    private static String xslt;

    @BeforeAll
    static void setup() {
        xslt = StringUtils.readFully(XsltTransformationTest.class
          .getResourceAsStream("/org/openrewrite/xml/XsltTransformationTest.xslt"));

        assertFalse(StringUtils.isBlank(xslt));
    }

    @DocumentExample
    @Test
    void transformFromParameter() {
        rewriteRun(
          spec -> spec.recipe(new XsltTransformation(xslt, null, "**/*.xml")),
          xml(
            """
              <configuration>
                  <activeRecipes>
                      <recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>
                  </activeRecipes>
              </configuration>
              """,
            """
              <configuration>
                  <activeRecipes>
                      <activeRecipe>org.openrewrite.java.cleanup.UnnecessaryThrows</activeRecipe>
                  </activeRecipes>
              </configuration>
              """
          )
        );
    }

    @Test
    void transformFromClasspathResource() {
        rewriteRun(
          spec -> spec.recipe(new XsltTransformation(null, "/org/openrewrite/xml/XsltTransformationTest.xslt", "**/*.xml")),
          xml(
            """
              <configuration>
                  <activeRecipes>
                      <recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>
                  </activeRecipes>
              </configuration>
              """,
            """
              <configuration>
                  <activeRecipes>
                      <activeRecipe>org.openrewrite.java.cleanup.UnnecessaryThrows</activeRecipe>
                  </activeRecipes>
              </configuration>
              """
          )
        );
    }
}
