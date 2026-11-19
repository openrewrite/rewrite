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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings("CheckTagEmptyBody")
class AddToTagTest implements RewriteTest {

    @Test
    void prefersElementIndentationToATrailingComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                  if ("local".equals(tag.getName())) {
                      doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<new><nested/></new>")));
                  }
                  return super.visitTag(tag, ctx);
              }
          })),
          xml(
            "<project>\n    <other>\n        <child/>\n    </other>\n" +
            "\t    <local>\n\t      <existing/>\n\t    <!-- closing comment -->\n\t    </local>\n</project>",
            "<project>\n    <other>\n        <child/>\n    </other>\n" +
            "\t    <local>\n\t      <existing/>\n\t    <!-- closing comment -->\n" +
            "\t      <new>\n\t        <nested/>\n\t      </new>\n\t    </local>\n</project>"
          )
        );
    }

    @Test
    void insertionUsesPrecedingElementBeforeADifferentlyIndentedSection() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                  doAfterVisit(new AddToTagVisitor<>(document.getRoot(),
                    Xml.Tag.build("<beta><child/></beta>"), new TagNameComparator()));
                  return super.visitDocument(document, ctx);
              }
          })),
          xml(
            "<project>\n  <alpha/>\n\n\t<zeta/>\n</project>",
            "<project>\n  <alpha/>\n  <beta>\n    <child/>\n  </beta>\n\n\t<zeta/>\n</project>"
          )
        );
    }

    @Test
    void expandsEmptyTagAtItsExistingIndentation() {
        rewriteRun(
          spec -> spec
            .allSources(source -> source.markers(new NamedStyles(randomId(), "spaces", "Spaces", null,
              emptySet(), singletonList(TabsAndIndentsStyle.DEFAULT))))
            .recipe(toRecipe(() -> new XmlVisitor<>() {
                @Override
                public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    if ("configuration".equals(tag.getName())) {
                        doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<release>25</release>")));
                    }
                    return super.visitTag(tag, ctx);
                }
            })),
          xml(
            "<project>\n  <build>\n\t        <configuration/>\n  </build>\n</project>",
            "<project>\n  <build>\n\t        <configuration>\n" +
            "\t          <release>25</release>\n\t        </configuration>\n  </build>\n</project>"
          )
        );
    }

    @Test
    void replacementKeepsExistingIndentation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                  if ("configuration".equals(tag.getName())) {
                      doAfterVisit(new AddOrUpdateChild<>(tag, Xml.Tag.build("<release>25</release>")));
                  }
                  return super.visitTag(tag, ctx);
              }
          })),
          xml(
            "<project>\n  <configuration>\n\t    <release>17</release>\n  </configuration>\n</project>",
            "<project>\n  <configuration>\n\t    <release>25</release>\n  </configuration>\n</project>"
          )
        );
    }

    @Test
    void followsSpaceIndentedNeighboursInTabStyledDocument() {
        rewriteRun(
          spec -> spec
            .allSources(source -> source.markers(new NamedStyles(randomId(), "tabs", "Tabs", null,
              emptySet(), singletonList(TabsAndIndentsStyle.DEFAULT.withUseTabCharacter(true)))))
            .recipe(toRecipe(() -> new XmlVisitor<>() {
                @Override
                public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    if ("dependencies".equals(tag.getName())) {
                        doAfterVisit(new AddToTagVisitor<>(tag,
                          Xml.Tag.build("<dependency><artifactId>new</artifactId></dependency>")));
                    }
                    return super.visitTag(tag, ctx);
                }
            })),
          xml(
            "<project>\n" +
            "\t<untouched>\n\t\t<value/>\n\t</untouched>\n" +
            "  <dependencies>\n" +
            "    <dependency>\n      <artifactId>old</artifactId>\n    </dependency>\n" +
            "  </dependencies>\n</project>",
            "<project>\n" +
            "\t<untouched>\n\t\t<value/>\n\t</untouched>\n" +
            "  <dependencies>\n" +
            "    <dependency>\n      <artifactId>old</artifactId>\n    </dependency>\n" +
            "    <dependency>\n      <artifactId>new</artifactId>\n    </dependency>\n" +
            "  </dependencies>\n</project>"
          )
        );
    }

    @Test
    void followsMixedTabAndSpaceNeighboursAtTheirActualDepth() {
        rewriteRun(
          spec -> spec
            .allSources(source -> source.markers(new NamedStyles(randomId(), "spaces", "Spaces", null,
              emptySet(), singletonList(TabsAndIndentsStyle.DEFAULT))))
            .recipe(toRecipe(() -> new XmlVisitor<>() {
                @Override
                public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    if ("configuration".equals(tag.getName())) {
                        doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<release>25</release>")));
                    }
                    return super.visitTag(tag, ctx);
                }
            })),
          xml(
            "<project>\n  <build>\n\t        <configuration>\n" +
            "\t          <encoding>UTF-8</encoding>\n" +
            "\t        </configuration>\n  </build>\n</project>",
            "<project>\n  <build>\n\t        <configuration>\n" +
            "\t          <encoding>UTF-8</encoding>\n" +
            "\t          <release>25</release>\n" +
            "\t        </configuration>\n  </build>\n</project>"
          )
        );
    }

    @DocumentExample
    @Test
    void addElement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().stream().noneMatch(c -> c.getAttributes().stream()
                    .anyMatch(attr -> "id".equals(attr.getKey().getName()) && "myBean2".equals(attr.getValueAsString())))) {
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
                  if (x.getRoot().getChildren().getFirst().getChildren().size() == 0) {
                      doAfterVisit(new AddToTagVisitor<>((Xml.Tag) requireNonNull(x.getRoot().getContent()).getFirst(),
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
                  if (x.getRoot().getChildren().stream().noneMatch(c -> "apple".equals(c.getName()))) {
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

    @Test
    void doesNotAddSemanticallyEqualDuplicate() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<bean id=\"myBean\"/>")));
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans>
                <bean id="myBean"/>
              </beans>
              """
          )
        );
    }

    @Test
    void doesNotAddDuplicateIgnoringAttributeOrder() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new AddToTagVisitor<>(x.getRoot(),
                          Xml.Tag.build("<bean class=\"C\" id=\"myBean\"/>")));
                  return super.visitDocument(x, ctx);
              }
          })),
          xml(
            """
              <beans>
                <bean id="myBean" class="C"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addsSemanticallyEqualDuplicateWhenAllowDuplicatesTrue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().size() == 1) {
                      doAfterVisit(new AddToTagVisitor<>(x.getRoot(),
                              Xml.Tag.build("<bean id=\"myBean\"/>"), true));
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
                <bean id="myBean"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addsWhenChildrenShareNameButDifferentAttributes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  doAfterVisit(new AddToTagVisitor<>(x.getRoot(), Xml.Tag.build("<bean id=\"myBean2\"/>")));
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    void preserveNonTagContent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new XmlVisitor<>() {
              @Override
              public Xml visitDocument(Xml.Document x, ExecutionContext ctx) {
                  if (x.getRoot().getChildren().stream().noneMatch(c -> "apple".equals(c.getName()))) {
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
