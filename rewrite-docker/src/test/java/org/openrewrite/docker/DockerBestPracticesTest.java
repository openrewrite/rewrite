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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class DockerBestPracticesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.docker.DockerBestPractices");
    }

    @DocumentExample
    @Test
    void appliesBestPractices() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ADD app.jar /app/
              RUN apt-get update
              RUN apt-get install -y curl
              ENTRYPOINT /app/start.sh
              """,
            """
              ~~(EOL: ubuntu:20.04 (ended 2025-05-31, suggest plucky (26.04)))~~>~~(Missing HEALTHCHECK instruction)~~>FROM ubuntu:20.04
              COPY app.jar /app/
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              ENTRYPOINT ["/app/start.sh"]
              USER appuser
              """
          )
        );
    }
}
