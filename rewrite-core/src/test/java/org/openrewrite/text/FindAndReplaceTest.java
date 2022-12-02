package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class FindAndReplaceTest implements RewriteTest {

    @Test
    void defaultNonRegex() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null, null)),
          text("""
          This is text.
          """, """
          This is textG
          """)
        );
    }

    @Test
    void regexReplace() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", true, null)),
          text("""
          This is text.
          """, """
          GGGGGGGGGGGGG
          """)
        );
    }

    @Test
    void captureGroups() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("This is ([^.]+).", "I like $1.", true, null)),
          text("""
          This is text.
          """, """
          I like text.
          """)
        );
    }
}