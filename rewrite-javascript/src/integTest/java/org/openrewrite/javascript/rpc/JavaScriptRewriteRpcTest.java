/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.SourceSpecs.text;

@Disabled
public class JavaScriptRewriteRpcTest implements RewriteTest {
    JavaScriptRewriteRpc client;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.validateRecipeSerialization(false);
    }

    @BeforeEach
    void before() throws InterruptedException {
        this.client = JavaScriptRewriteRpc.start(
          Environment.builder().build(),
          "node",
          // Uncomment this to debug the server
//          "--inspect-brk",
          "./rewrite/dist/src/rpc/server.js"
        );
        client.batchSize(20).timeout(Duration.ofMinutes(10));

        Thread.sleep(3000);
        System.out.println("Sending message");
    }

    @AfterEach
    void after() {
        client.shutdown();
    }

    @Test
    void getRecipes() {
        installRecipes();
        assertThat(client.getRecipes()).isNotEmpty();
    }

    @Test
    void prepareRecipe() {
        installRecipes();
        Recipe recipe = client.prepareRecipe("org.openrewrite.npm.change-version",
          Map.of("version", "1.0.0"));
        assertThat(recipe.getDescriptor().getDisplayName()).isEqualTo("Change version in `package.json`");
    }

    @Test
    void print() {
        rewriteRun(
          text(
            "Hello Jon!",
            spec -> spec.beforeRecipe(text ->
              assertThat(client.print(text)).isEqualTo("Hello Jon!"))
          )
        );
    }

    @Test
    void runRecipe() {
        installRecipes();
        rewriteRun(
          spec -> spec.recipe(client.prepareRecipe("org.openrewrite.npm.change-version",
            Map.of("version", "1.0.0"))),
          json(
            """
              {
                "name": "my-project",
                "version": "0.0.1"
              }
              """,
            """
              {
                "name": "my-project",
                "version": "1.0.0"
              }
              """,
            spec -> spec.path("package.json")
          )
        );
    }

    private void installRecipes() {
        assertThat(client.installRecipes("@openrewrite/recipes-npm")).isEqualTo(1);
    }
}
