package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UsePrecompiledRegExpForReplaceAllTest implements RewriteTest {

    @Test
    void replaceSimpleVar() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()),
          java(
            """
             class A {
                 public void replace(){
                     String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                     String changed = init.replaceAll("/[@]/g,", "_");
                 }
             }
             """,
            """
              class A {
                  private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar = Pattern.compile("/[@]/g,");
                  public void replace(){
                      String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                      String changed = openRewriteReplaceAllPatternVar.matcher(init).replaceAll("_");
                  }
              }
              """
          )
        );
    }
}
