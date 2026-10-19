/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyMap;
import static org.openrewrite.python.Assertions.python;

class PythonRecipeRunTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    /**
     * When a run ends, the JVM asks the Python process for the execution context, which is the one
     * object it fetches from there that is no tree.
     */
    @Test
    void recipeOfThePythonProcessRunToCompletion() {
        Recipe removePass = PythonRewriteRpc.getOrStart().prepareRecipe("org.openrewrite.python.RemovePass", emptyMap());
        rewriteRun(
          spec -> spec.recipe(removePass),
          python(
            """
              def foo():
                  pass
                  x = 1
              """,
            """
              def foo():
                  x = 1
              """
          )
        );
    }
}
