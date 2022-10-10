package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

public class RenameFileTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RenameFile("**/hello.txt", "goodbye.txt"));
    }

    @Test
    void hasFileMatch() {
        rewriteRun(
          text(
            "hello world",
            "hello world",
            spec -> {
                AtomicReference<SourceFile> before = new AtomicReference<>();
                spec
                  .path("a/b/hello.txt")
                  .beforeRecipe(before::set)
                  .afterRecipe(pt -> {
                      assertThat(pt.getSourcePath()).isEqualTo(Paths.get("a/b/goodbye.txt"));
                      assertThat(new Result(before.get(), pt, Collections.emptyList()).diff())
                        .isEqualTo(
                          """
                                diff --git a/a/b/hello.txt b/a/b/goodbye.txt
                                similarity index 0%
                                rename from a/b/hello.txt
                                rename to a/b/goodbye.txt
                            """ + "\n"
                        );
                  });
            }
          )
        );
    }

    @Test
    void hasNoFileMatch() {
        rewriteRun(
          text("hello world", spec -> spec.path("a/b/goodbye.txt"))
        );
    }
}
