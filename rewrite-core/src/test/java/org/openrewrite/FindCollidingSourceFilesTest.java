package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

public class FindCollidingSourceFilesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindCollidingSourceFiles());
    }

    @Test
    void findsCollision() {
        rewriteRun(
          text("", "~~(Duplicate source file foo.txt)~~>", spec -> spec.path("foo.txt")),
          text("", "~~(Duplicate source file foo.txt)~~>", spec -> spec.path("foo.txt")),
          text("", spec -> spec.path("bar.txt"))
        );
    }
}
