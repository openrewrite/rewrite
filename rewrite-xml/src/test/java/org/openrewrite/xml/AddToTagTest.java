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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings("CheckTagEmptyBody")
class AddToTagTest implements RewriteTest {

    @Test
    void addElement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().stream().noneMatch(c -> c.getAttributes().stream()
                    .anyMatch(attr -> attr.getKey().getName().equals("id") && attr.getValueAsString().equals("myBean2")))) {
                      doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<bean id=\"myBean2\"/>")));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans>
                <bean id="myBean"/>
              </beans>
              """,
            """
              <beans>
                <bean id="myBean"/>
                <bean id="myBean2"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addElementToSlashClosedTag() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().get(0).getChildren().size() == 0) {
                      doAfterVisit(new AddToTagVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).get(0),
                        Xml.Tag.build("<property name=\"myprop\" ref=\"collaborator\"/>")));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans >
                <bean id="myBean" />
              </beans>
              """,
            """
              <beans >
                <bean id="myBean">
                  <property name="myprop" ref="collaborator"/>
                </bean>
              </beans>
              """
          )
        );
    }

    @Test
    void addElementToEmptyTagOnSameLine() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().isEmpty()) {
                      doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<bean id=\"myBean\"/>"), new TagNameComparator()));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans></beans>
              """,
            """
              <beans>
                <bean id="myBean"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addElementInOrder() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().stream().noneMatch(c -> c.getName().equals("apple"))) {
                      doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<apple/>"), new TagNameComparator()));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans >
                <banana/>
              </beans>
              """,
            """
              <beans >
                <apple/>
                <banana/>
              </beans>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    void preserveNonTagContent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().stream().noneMatch(c -> c.getName().equals("apple"))) {
                      doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<apple/>"), new TagNameComparator()));
                  }
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans>
                <!-- comment -->
                <?processing instruction?>
                <banana/>
              </beans>
              """,
            """
              <beans>
                <apple/>
                <!-- comment -->
                <?processing instruction?>
                <banana/>
              </beans>
              """
          )
        );
    }
}
