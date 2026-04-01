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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class ChangeTagValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void rewriteEmptyTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              null, "2.0", null)),
          xml(
            """
              <dependency>
                  <version/>
              </dependency>
              """,
            """
              <dependency>
                  <version>2.0</version>
              </dependency>
              """
          )
        );
    }

    @Test
    void matchExistingTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "1.0", "2.0", null)),
          xml(
            """
              <dependency>
                  <version>1.0</version>
              </dependency>
              """,
            """
              <dependency>
                  <version>2.0</version>
              </dependency>
              """
          )
        );
    }

    @Test
    void noMatchExistingTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "1.0", "2.0", null)),
          xml(
            """
              <dependency>
                  <version>3.0</version>
              </dependency>
              """
          )
        );
    }

    @Test
    void rewriteTagValueSubstring() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "SNAPSHOT", "RELEASE", true)
          ),
          xml(
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.2.3-SNAPSHOT</version>
              </dependency>
              """,
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.2.3-RELEASE</version>
              </dependency>
              """
          )
        );
    }


    @Test
    void appendTagValue() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "$", "-RELEASE", true)
          ),
          xml(
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.2.3</version>
              </dependency>
              """,
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.2.3-RELEASE</version>
              </dependency>
              """
          )
        );
    }


    @Test
    void replaceWithCapturingGroups() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeTagValue("/dependency/version",
              "(\\d).(\\d).(\\d)", "$1.$3.4", true)
          ),
          xml(
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.2.3</version>
              </dependency>
              """,
            """
              <dependency>
                  <group>com.company.project</group>
                  <artifact>artifact</artifact>
                  <version>1.3.4</version>
              </dependency>
              """
          )
        );
    }

    @Test
    void replacedExactlyOnce() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagValue(
            "/tag",
            "(aaa)",
            "$1-aaa",
            true)),
          xml(
            "<tag>aaa</tag>",
            "<tag>aaa-aaa</tag>"
          )
        );
    }

    @Nested
    class FindAndReplaceTagTextVisitorTest {

        @Test
        void findAndReplace() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                  @Override
                  public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                      doAfterVisit(new ChangeTagValue.RegexReplaceVisitor<>(
                        (Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(),
                        "2.0",
                        "3.0")
                      );
                      return super.visitDocument(x, ctx);
                  }
              })),
              xml(
                """
                  <dependency>
                      <version>
                          2.0
                      </version>
                  </dependency>
                  """,
                """
                  <dependency>
                      <version>
                          3.0
                      </version>
                  </dependency>
                  """
              )
            );
        }

        @Test
        void changeContentSubstring() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                  @Override
                  public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                      doAfterVisit(new ChangeTagValue.RegexReplaceVisitor<>(
                        requireNonNull(x.getRoot()),
                        "SNAPSHOT",
                        "RELEASE")
                      );
                      return super.visitDocument(x, ctx);
                  }
              })),
              xml(
                "<tag>1.2.3-SNAPSHOT</tag>",
                "<tag>1.2.3-RELEASE</tag>"
              )
            );
        }

        @Test
        void doNothingIfPatternDoesNotMatch() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                  @Override
                  public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                      doAfterVisit(new ChangeTagValue.RegexReplaceVisitor<>(
                        (Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(),
                        "7.0",
                        "8.0")
                      );
                      return super.visitDocument(x, ctx);
                  }
              })),
              xml(
                """
                  <executions>
                      <execution>
                          <configs/>
                          <goals/>
                      </execution>
                  </executions>
                  """
              )
            );
        }

        @Test
        void doNothingForNonTextNotFound() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                  @Override
                  public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                      doAfterVisit(new ChangeTagValue.RegexReplaceVisitor<>(
                        (Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(),
                        "7.0",
                        "8.0")
                      );
                      return super.visitDocument(x, ctx);
                  }
              })),
              xml(
                """
                  <dependency>
                      <version>2.0</version>
                  </dependency>
                  """
              )
            );
        }

        @Test
        void skipsChildIfNotText() {
            rewriteRun(
              spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
                  @Override
                  public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                      doAfterVisit(new ChangeTagValue.RegexReplaceVisitor<>(
                        (Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(),
                        "invalid",
                        "2.0")
                      );
                      return super.visitDocument(x, ctx);
                  }
              })),
              xml(
                """
                  <dependency>
                      <version><invalid/></version>
                  </dependency>
                  """
              )
            );
        }
    }
}
