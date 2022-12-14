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

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class RemoveContentTest implements RewriteTest {

    @Test
    void removeContent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (ctx.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                      doAfterVisit(new RemoveContentVisitor<>(requireNonNull(x.getRoot().getContent()).get(1), false));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <dependency>
                  <groupId>group</groupId>
                  <version/>
              </dependency>
              """,
            """
              <dependency>
                  <groupId>group</groupId>
              </dependency>
              """
          )
        );
    }

    @Test
    void removeAncestorsThatBecomeEmpty() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (ctx.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                      doAfterVisit(new RemoveContentVisitor<>(requireNonNull(x.getRoot().getChildren()).get(1)
                        .getChildren().get(0).getChildren().get(0), true));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <project>
                  <name>my.company</name>
                  <dependencyManagement>
                      <dependencies>
                          <groupId>group</groupId>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <name>my.company</name>
              </project>
              """
          )
        );
    }

    @Test
    void rootChangedToEmptyTagIfLastRemainingTag() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (ctx.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                      doAfterVisit(new RemoveContentVisitor<>(requireNonNull(x.getRoot().getChildren()).get(0)
                        .getChildren().get(0).getChildren().get(0), true));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <project>
                  <dependencyManagement>
                      <dependencies>
                          <groupId>group</groupId>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project/>
              """
          )
        );
    }
}
