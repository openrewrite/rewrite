package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NotUsesTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NotUsesType("java.lang.String", true));
    }

    @DocumentExample
    @Test
    void doesNotUseType() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1),
          java(
            """
            class Foo{
                int bla = 123;
            }
            """
          ));
    }

    @Test
    void doesUseType() {
        rewriteRun(
          java(
            """
            class Foo{
                String bla = "bla";
            }
            """));
    }

}
