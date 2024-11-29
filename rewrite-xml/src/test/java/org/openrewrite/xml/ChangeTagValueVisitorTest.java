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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class ChangeTagValueVisitorTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeTagValue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    (Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                    null,
                    "2.0",
                    Boolean.FALSE)
                  );
                  return super.visitDocument(x, ctx);
              }
          })),
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
    void preserveOriginalFormatting() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    (Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                    "2.0",
                    "3.0",
                    Boolean.TRUE)
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
    void noChangeWhenNewValueIsSameAsOldValue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    requireNonNull(x.getRoot()),
                    "unchanged",
                    "unchanged", Boolean.TRUE)
                  );
                  return super.visitDocument(x, ctx);
              }
          })),
          xml("<tag>unchanged</tag>")
        );
    }


    @Test
    void changeContentSubstring() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    requireNonNull(x.getRoot()),
                    "SNAPSHOT",
                    "RELEASE", Boolean.TRUE)
                  );
                  return super.visitDocument(x, ctx);
              }
          })),
          xml("<tag>1.2.3-SNAPSHOT</tag>", "<tag>1.2.3-RELEASE</tag>")
        );
    }

    @Test
    void doNothingIfPatternNotProvided() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    (Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                    null,
                    "2.0",
                    Boolean.TRUE)
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
    void alwaysReplaceChild() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    (Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                    null,
                    "2.0",
                    Boolean.FALSE)
                  );
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <dependency>
                   <version><invalid/></version>
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
    void regexSkipsChild() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(
                    (Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                    "invalid",
                    "2.0",
                    Boolean.TRUE)
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