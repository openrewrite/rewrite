package org.openrewrite.groovy.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

public class GStringCurlyBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GStringCurlyBraces());
    }

    @Test
    void basic() {
        rewriteRun(
          groovy("""
            def name = 'world'
            "Hello $name!"
            """,
            """
            def name = 'world'
            "Hello ${name}!"
            """)
        );
    }

    @Test
    void fieldAccess() {
        rewriteRun(
          groovy("""
            def to = [ you : 'world']
            "Hello $to.you!"
            """,
            """
            def to = [ you : 'world']
            "Hello ${to.you}!"
            """)
        );
    }
}
