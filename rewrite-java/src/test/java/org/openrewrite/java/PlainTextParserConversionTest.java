/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class PlainTextParserConversionTest implements RewriteTest {

    private static final Recipe markClassDeclaration = toRecipe(() -> new JavaVisitor<>() {
        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            return Markup.info(classDecl, "test");
        }
    });

    @DocumentExample
    @Test
    void markersAreAddedWhenConvertingToTextLst() {
        rewriteRun(
          spec -> {
              spec.afterRecipe(run -> {
                  SourceFile result = run.getChangeset().getAllResults().getFirst().getAfter();
                  assertThat(result).isInstanceOf(PlainText.class);
                  assertThat(result.getMarkers().getMarkers()).hasSize(3);
                  assertThat(result.getMarkers().findAll(AlreadyReplaced.class)).isNotEmpty();
                  assertThat(result.getMarkers().findAll(Markup.class)).isNotEmpty();
                  assertThat(result.getMarkers().findFirst(RecipesThatMadeChanges.class).get().getRecipes()).hasSize(2);
              });
              spec.recipes(markClassDeclaration, new FindAndReplace("class", "public class", null, null, null, null, null, null));
          },
          java(
            """
              package com.example;
              
              class Test {
              }
              """,
            """
              ~~(test)~~>package com.example;
              
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void markersAreKeptWhenNoConversionToTextLst() {
        rewriteRun(
          spec ->
            spec.recipes(markClassDeclaration, new FindAndReplace("doesNotExist", "willNotAppear", null, null, null, null, null, null)),
          java(
            """
              package com.example;
              
              class Test {
              }
              """,
            """
              package com.example;
              
              /*~~(test)~~>*/class Test {
              }
              """
          )
        );
    }
}
