/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.docker.Assertions.docker;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AssertionsTest implements RewriteTest {

    @Test
    void catchesATreeThatPrintsCorrectlyButModelsTheWrongThing() {
        assertThatThrownBy(() -> rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new DockerIsoVisitor<ExecutionContext>() {
              @Override
              public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                  // Appending the tag to the image name prints as "alpine:3.14", but leaves the tag
                  // unmodelled, so From#getTag stays null.
                  Docker.Literal name = (Docker.Literal) from.getImageName().getContents().getFirst();
                  if (name.getText().contains(":")) {
                      return from;
                  }
                  return from.withImageName(from.getImageName().withContents(
                    singletonList(name.withText(name.getText() + ":3.14"))));
              }
          })),
          docker(
            """
              FROM alpine
              """,
            """
              FROM alpine:3.14
              """
          )
        )).isInstanceOf(AssertionError.class)
          .hasMessageContaining("Expected the tree to model what it prints");
    }

}
