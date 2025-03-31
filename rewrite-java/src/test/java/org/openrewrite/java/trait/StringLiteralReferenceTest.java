package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StringLiteralReferenceTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new StringLiteralReference.Provider().getMatcher()
          .asVisitor(stringLiteralReference -> SearchResult.found(stringLiteralReference.getTree(), stringLiteralReference.getValue()))));
    }

    @SuppressWarnings("SpringXmlModelInspection")
    @Test
    @DocumentExample
    void xmlConfiguration() {
        rewriteRun(
          java(
            //language=java
            """
              class Test {
                  String ref = "java.lang.String";
              }
              """,
            //language=java
            """
              class Test {
                  String ref = /*~~(java.lang.String)~~>*/"java.lang.String";
              }
              """
          )
        );
    }
}