package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class JavaTemplateTest8Test implements RewriteTest {

    @Test
    void parameterizedMatch() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<String>)}")
          .doBeforeParseTemplate(System.out::println)
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  if (template.matches(getCursor())) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<String> s;
                  List<Integer> n;
              }
              """,
            """
              import java.util.List;
              class Test {
                  /*~~>*/List<String> s;
                  List<Integer> n;
              }
              """
          )
        );
    }
}
