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
package org.openrewrite.docker.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class DockerImageReferenceTest implements RewriteTest {

    @DocumentExample
    @Test
    void updatesUnpinnedImageReferencesAnywhere() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerImageReference.Matcher().asVisitor((ref, ctx) ->
              ref.isUnpinned() ? ref.withTag("3.19") : ref.getTree())
          )),
          docker(
            """
              FROM alpine AS builder
              RUN make
              FROM alpine
              COPY --from=builder /out /app
              COPY --from=alpine /lib /app/lib
              ADD --from=alpine:latest /etc /app/etc
              """,
            """
              FROM alpine:3.19 AS builder
              RUN make
              FROM alpine:3.19
              COPY --from=builder /out /app
              COPY --from=alpine:3.19 /lib /app/lib
              ADD --from=alpine:3.19 /etc /app/etc
              """
          )
        );
    }

    @Test
    void matchesEveryImageReferenceButSkipsStageReferences() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerImageReference.Matcher().asVisitor((ref, ctx) ->
              SearchResult.found(ref.getTree()))
          )),
          docker(
            """
              FROM alpine AS builder
              FROM alpine
              COPY --from=builder /out /app
              COPY --from=nginx:1.25 /web /app/web
              """,
            """
              ~~>FROM alpine AS builder
              ~~>FROM alpine
              COPY --from=builder /out /app
              ~~>COPY --from=nginx:1.25 /web /app/web
              """
          )
        );
    }

    @Test
    void filtersByImageNameAcrossLocations() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerImageReference.Matcher().imageName("nginx").asVisitor((ref, ctx) ->
              SearchResult.found(ref.getTree()))
          )),
          docker(
            """
              FROM nginx:1.20
              COPY --from=nginx:1.25 /web /app
              COPY --from=redis:6 /data /seed
              """,
            """
              ~~>FROM nginx:1.20
              ~~>COPY --from=nginx:1.25 /web /app
              COPY --from=redis:6 /data /seed
              """
          )
        );
    }

    @Test
    void replacesWholeReferenceViaWithImageReference() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerImageReference.Matcher().imageName("nginx").asVisitor((ref, ctx) ->
              "1.27".equals(ref.getTag().orElse(null)) ? ref.getTree() : ref.withImageReference("nginx:1.27"))
          )),
          docker(
            """
              FROM nginx:1.20
              COPY --from=nginx:1.25 /web /app
              """,
            """
              FROM nginx:1.27
              COPY --from=nginx:1.27 /web /app
              """
          )
        );
    }
}
