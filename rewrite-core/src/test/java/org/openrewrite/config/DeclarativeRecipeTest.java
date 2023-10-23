package org.openrewrite.config;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.List;
import java.util.Map;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class DeclarativeRecipeTest implements RewriteTest {

    @Test
    void precondition() {
        rewriteRun(
            spec -> {
                spec.validateRecipeSerialization(false);
                DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", null,
                  null, null, true, null);
                dr.addPrecondition(
                  toRecipe(() -> new PlainTextVisitor<>() {
                      @Override
                      public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                          if("1".equals(text.getText())) {
                              return SearchResult.found(text);
                          }
                          return text;
                      }
                  })
                );
                dr.addUninitialized(
                  new ChangeText("2")
                );
                dr.addUninitialized(
                  new ChangeText("3")
                );
                dr.initialize(List.of(), Map.of());
                spec.recipe(dr);
            },
            text("1","3"),
            text("2")
        );
    }
}
