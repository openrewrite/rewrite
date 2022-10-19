package org.openrewrite.quark;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.other;

public class QuarkTest implements RewriteTest {

    @Test
    void renderMarkersOnQuarks() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
              @Override
              public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext ctx) {
                  return SearchResult.found(sourceFile);
              }
          })),
          other(
            "this text will not be read because this is a quark",
            "~~>⚛⚛⚛ The contents of this file are not visible. ⚛⚛⚛"
          )
        );
    }
}
