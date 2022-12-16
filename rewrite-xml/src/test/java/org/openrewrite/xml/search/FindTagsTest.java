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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class FindTagsTest implements RewriteTest {

    @Test
    void simpleElement() {
        rewriteRun(
          spec -> spec.recipe(new FindTags("/dependencies/dependency")),
          xml(
            """
              <dependencies>
                  <dependency>
                      <artifactId scope="compile">org.openrewrite</artifactId>
                  </dependency>
              </dependencies>
              """,
            """
              <dependencies>
                  <!--~~>--><dependency>
                      <artifactId scope="compile">org.openrewrite</artifactId>
                  </dependency>
              </dependencies>
              """
          )
        );
    }

    @Test
    void wildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindTags("/dependencies/*")),
          xml(
            """
              <dependencies>
                  <dependency>
                      <artifactId scope="compile">org.openrewrite</artifactId>
                  </dependency>
              </dependencies>
              """,
            """
              <dependencies>
                  <!--~~>--><dependency>
                      <artifactId scope="compile">org.openrewrite</artifactId>
                  </dependency>
              </dependencies>
              """
          )
        );
    }

    @Test
    void noMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindTags("/dependencies/dne")),
          xml(
            """
              <dependencies>
                  <dependency>
                      <artifactId scope="compile">org.openrewrite</artifactId>
                  </dependency>
              </dependencies>
              """
          )
        );
    }

    @Test
    void staticFind() {
        //noinspection ConstantConditions
        rewriteRun(
          xml(
            """
            <dependencies>
                <dependency>
                    <artifactId scope="compile">org.openrewrite</artifactId>
                </dependency>
            </dependencies>
            """,
            spec -> spec.beforeRecipe(xml -> assertThat(FindTags.find(xml, "/dependencies/dependency"))
              .isNotEmpty()
              .allMatch(tag -> tag instanceof Xml.Tag))
          )
        );
    }
}
