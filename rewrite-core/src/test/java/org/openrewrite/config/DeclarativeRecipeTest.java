/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                  null, null, 2, true, null);
                dr.addPrecondition(
                  toRecipe(() -> new PlainTextVisitor<>() {
                      @Override
                      public PlainText visitText(PlainText text, ExecutionContext ctx) {
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

    @Test
    void yamlPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionTest
            preconditions:
              - org.openrewrite.text.Find:
                  find: 1
            recipeList:
              - org.openrewrite.text.ChangeText:
                 toText: 2
              - org.openrewrite.text.ChangeText:
                 toText: 3
            """, "org.openrewrite.PreconditionTest"),
          text("1","3"),
          text("2")
        );
    }
}
