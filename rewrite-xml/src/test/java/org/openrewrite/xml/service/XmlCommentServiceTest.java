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
package org.openrewrite.xml.service;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Comments;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.trait.Comments.Placement.FIRST_CHILD;
import static org.openrewrite.xml.Assertions.xml;

class XmlCommentServiceTest implements RewriteTest {

    @Test
    void firstChildPlacementCoversTheCommentsOpeningTheTag() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlIsoVisitor<>() {
              @Override
              public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                  if (!"properties".equals(tag.getName())) {
                      return super.visitTag(tag, ctx);
                  }
                  Comments comments = Comments.of(getCursor());
                  return comments.hasComment(" capped ", FIRST_CHILD) ?
                    comments.removeComment(" capped ", FIRST_CHILD) :
                    tag;
              }
          })),
          xml(
            """
              <project>
                  <properties>
                      <!-- capped -->
                      <java.version>24</java.version>
                      <!-- capped -->
                      <kotlin.version><!-- capped -->1.9.0</kotlin.version>
                  </properties>
                  <profiles>
                      <profile>
                          <properties>
                              <!-- capped -->
                          </properties>
                      </profile>
                  </profiles>
              </project>
              """,
            """
              <project>
                  <properties>
                      <java.version>24</java.version>
                      <!-- capped -->
                      <kotlin.version><!-- capped -->1.9.0</kotlin.version>
                  </properties>
                  <profiles>
                      <profile>
                          <properties>
                          </properties>
                      </profile>
                  </profiles>
              </project>
              """
          )
        );
    }
}
