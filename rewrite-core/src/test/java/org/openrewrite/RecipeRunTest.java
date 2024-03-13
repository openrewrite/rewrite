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
import org.openrewrite.text.FindAndReplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

public class RecipeRunTest implements RewriteTest {
    @Test
    void printDatatable() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new FindAndReplace("replace_me", "replacement", null, null, null, null, null))
            .afterRecipe(recipeRun -> {
                StringBuilder output = new StringBuilder();
                final String dataTableName = SourcesFileResults.class.getName();
                RecipeRun.exportCsv(new InMemoryExecutionContext(), recipeRun.getDataTable(dataTableName),
                  s -> output.append(s).append("\n"), recipeRun.getDataTableRows(dataTableName));
                assertThat(output.toString()).isEqualTo("""
                  "Source path before the run","Source path after the run","Parent of the recipe that made changes","Recipe that made changes","Estimated time saving","Cycle"
                  "The source path of the file before the run.","A recipe may modify the source path. This is the path after the run.","In a hierarchical recipe, the parent of the recipe that made a change. Empty if this is the root of a hierarchy or if the recipe is not hierarchical at all.","The specific recipe that made a change.","An estimated effort that a developer to fix manually instead of using this recipe, in unit of seconds.","The recipe cycle in which the change was made."
                  "file.txt","file.txt","","org.openrewrite.text.FindAndReplace","300","1"
                  """);
            }), text("""
            replace_me
            """, """
            replacement
            """));
    }
}
