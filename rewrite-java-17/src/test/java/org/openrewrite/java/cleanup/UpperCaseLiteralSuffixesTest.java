package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class UpperCaseLiteralSuffixesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpperCaseLiteralSuffixes());
    }

    @Test
    void useUppercaseLiteralSuffix() {
        rewriteRun(
          java("""
            class Test {
                long lp = 1l;
                Long l = 100l;
                Double d = 100.0d;
                Float f = 100f;
                Integer i = 0;
                Long l2 = 0x100000000l;
                String s = "hello";
            }
            """,
            """
            class Test {
                long lp = 1L;
                Long l = 100L;
                Double d = 100.0D;
                Float f = 100F;
                Integer i = 0;
                Long l2 = 0x100000000L;
                String s = "hello";
            }
            """)
        );
    }
}
