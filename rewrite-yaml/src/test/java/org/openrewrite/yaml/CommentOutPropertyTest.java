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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class CommentOutPropertyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CommentOutProperty("management.metrics.binders.files.enabled", "some comments"));
    }

    @DocumentExample("comment out a map entry")
    @Test
    void regular() {
        rewriteRun(
          spec -> spec.recipe(new CommentOutProperty("foo.bar.sequence.propertyA",
            "Some comments")),
          yaml(
            """
                foo:
                  bar:
                    sequence:
                      - name: name
                      - propertyA: fieldA
                      - propertyB: fieldB
                    scalar: value
              """,
            """
                foo:
                  bar:
                    sequence:
                      - name: name
                      # Some comments
                      # propertyA: fieldA
                      - propertyB: fieldB
                    scalar: value
              """
          )
        );
    }

    @DocumentExample("comment out entire sequence")
    @Test
    void commentSequence() {
        rewriteRun(
          spec -> spec.recipe(new CommentOutProperty("foo.bar.sequence",
            "Some comments")),
          yaml(
            """
              foo:
                bar:
                  sequence:
                    - name: name
                    - propertyA: fieldA
                    - propertyB: fieldB
                  scalar: value
              """,
            """
              foo:
                bar:
                  # Some comments
                  # sequence:
                  #   - name: name
                  #   - propertyA: fieldA
                  #   - propertyB: fieldB
                  scalar: value
              """
          )
        );
    }
}
