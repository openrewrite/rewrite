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
}