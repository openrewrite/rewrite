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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class PlainTextAsYamlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PlainTextAsYaml("**/*.cyml"));
    }

    @DocumentExample
    @Test
    void parsePlainTextAsYamlThenApplyYamlRecipe() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsYaml("**/*.cyml"),
            new ChangeKey("$.key", "newKey")
          ),
          text(
            """
              key: value
              """,
            """
              newKey: value
              """,
            spec -> spec.path("config.cyml")
          )
        );
    }

    @Test
    void convertsToYamlDocument() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> {
              SourceFile result = run.getChangeset().getAllResults().getFirst().getAfter();
              assertThat(result).isInstanceOf(Yaml.Documents.class);
          }).recipes(
            new PlainTextAsYaml("**/*.cyml"),
            new ChangeValue("$.nested.child", "newData", null)
          ),
          text(
            """
              key: value
              nested:
                child: data
              """,
            """
              key: value
              nested:
                child: newData
              """,
            spec -> spec.path("config.cyml")
          )
        );
    }

    @Test
    void doesNotMatchWhenPatternDoesNotMatch() {
        rewriteRun(
          text(
            """
              key: value
              """,
            spec -> spec.path("config.other")
          )
        );
    }

    @Test
    void changeValue() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsYaml("**/*.cyml"),
            new ChangeValue("$.nested.child", "newData", null)
          ),
          text(
            """
              key: value
              nested:
                child: data
              """,
            """
              key: value
              nested:
                child: newData
              """,
            spec -> spec.path("config.cyml")
          )
        );
    }
}
