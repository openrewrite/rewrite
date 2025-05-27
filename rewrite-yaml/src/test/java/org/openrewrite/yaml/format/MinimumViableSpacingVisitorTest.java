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
package org.openrewrite.yaml.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.yaml.Assertions.yaml;

class MinimumViableSpacingVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          toRecipe(() -> new YamlIsoVisitor<>() {
                @Override
                public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                    return new MinimumViableSpacingVisitor<>(null).visitDocuments(documents, ctx);
                }
            }
          )
        );
    }

    @Test
    void sameLineArrayNotChanged() {
        rewriteRun(
          yaml(
            """
              build:
                existing: value
                steps: [run: publish]
              """
          )
        );
    }

    @Test
    void nextLineArrayNotChanged() {
        rewriteRun(
          yaml(
            """
              build:
                existing: value
                steps:
                  - run: publish
              """
          )
        );
    }
}
