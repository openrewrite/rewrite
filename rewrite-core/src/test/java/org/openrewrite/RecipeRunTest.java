/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.text.FindAndReplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeRunTest implements RewriteTest {
    @DocumentExample
    @Test
    void printDatatable() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new FindAndReplace("replace_me", "replacement", null, null, null, null, null, null))
            .afterRecipe(recipeRun -> {
                StringBuilder output = new StringBuilder();
                final String dataTableName = SourcesFileResults.class.getName();
                RecipeRun.exportCsv(new InMemoryExecutionContext(), recipeRun.getDataTable(dataTableName),
                  s -> output.append(s).append("\n"), recipeRun.getDataTableRows(dataTableName));
                assertThat(output.toString()).contains("org.openrewrite.text.FindAndReplace");
            }), text(
                """
            replace_me
            """,
                """
            replacement
            """ ));
    }


    @Test
    void delegateRecipeWithOnComplete() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        rewriteRun(recipeSpec -> recipeSpec.recipe(new DelegatingRecipe()).executionContext(ctx).typeValidationOptions(TypeValidation.none()));
        assertThat(ctx.<String>getMessage("org.openrewrite.recipe.oncomplete")).isEqualTo("with delegate recipe.");
    }

    public static class DelegatingRecipe extends Recipe implements Recipe.DelegatingRecipe{

        @Override
        public String getDisplayName() {
            return "Test delegate recipe";
        }

        @Override
        public String getDescription() {
            return "Test onComplete with delegate recipe.";
        }

        @Override
        public Recipe getDelegate() {
            return new Recipe() {
                @Override
                public String getDisplayName() {
                    return "Actual recipe";
                }

                @Override
                public String getDescription() {
                    return "Actual recipe with onComplete.";
                }

                @Override
                public void onComplete(ExecutionContext ctx) {
                    ctx.putMessage("org.openrewrite.recipe.oncomplete", "with delegate recipe.");
                }
            };
        }
    }
}
