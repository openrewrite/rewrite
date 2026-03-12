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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

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
                  doAfterVisit(new ChangeTagValueVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(), "2.0"));
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
    void noChangeIfAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(), "2.0"));
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
    void preserveOriginalFormatting() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(), "3.0"));
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
    void doesNothingIfScopeIsNull() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new ChangeTagValueVisitor<>(null, "3.0"));
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
              """
          )
        );
    }

    @Test
    void removesTagIfValueIsNull() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  List<? extends Content> rootContent = x.getRoot().getContent();
                  Xml.Tag firstTag = rootContent != null && !rootContent.isEmpty() ? (Xml.Tag) rootContent.getFirst() : null;
                  doAfterVisit(new ChangeTagValueVisitor<>(firstTag, null));
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
              </dependency>
              """
          )
        );
    }
}
