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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class DockerCopyFromTest implements RewriteTest {

    @DocumentExample
    @Test
    void upgradesCopyFromTag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().imageName("nginx").tag("1.20").asVisitor((image, ctx) ->
              image.withTag("1.25"))
          )),
          docker(
            """
              FROM alpine
              COPY --from=nginx:1.20 /usr/share/nginx /app
              """,
            """
              FROM alpine
              COPY --from=nginx:1.25 /usr/share/nginx /app
              """
          )
        );
    }

    @Test
    void pinsUnpinnedCopyFromToSpecificVersion() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().imageName("alpine").asVisitor((image, ctx) ->
              image.isUnpinned() ? image.withTag("3.19") : image.getTree())
          )),
          docker(
            """
              FROM ubuntu
              COPY --from=alpine /lib /app/lib
              ADD --from=alpine:latest /etc /app/etc
              """,
            """
              FROM ubuntu
              COPY --from=alpine:3.19 /lib /app/lib
              ADD --from=alpine:3.19 /etc /app/etc
              """
          )
        );
    }

    @Test
    void preservesDigestWhenUpgradingTag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().imageName("nginx").tag("1.20").asVisitor((image, ctx) ->
              image.withTag("1.25"))
          )),
          docker(
            """
              FROM alpine
              COPY --from=nginx:1.20@sha256:abc123 /usr/share/nginx /app
              """,
            """
              FROM alpine
              COPY --from=nginx:1.25@sha256:abc123 /usr/share/nginx /app
              """
          )
        );
    }

    @Test
    void matchesExternalImageInCopyFrom() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().imageName("nginx").asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isFalse();
                assertThat(image.getImageName()).contains("nginx");
                assertThat(image.getTag()).contains("latest");
                assertThat(image.getDigest()).isEmpty();
                assertThat(image.isUnpinned()).isTrue();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=nginx:latest /usr/share/nginx /app
              """,
            """
              FROM alpine
              ~~>COPY --from=nginx:latest /usr/share/nginx /app
              """
          )
        );
    }

    @Test
    void identifiesStageReferenceByName() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.getFromValue()).contains("builder");
                assertThat(image.isStageReference()).isTrue();
                assertThat(image.getImageName()).isEmpty();
                assertThat(image.getTag()).isEmpty();
                assertThat(image.isUnpinned()).isFalse();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS builder
              RUN make
              FROM alpine
              COPY --from=builder /out /app
              """,
            """
              FROM alpine AS builder
              RUN make
              FROM alpine
              ~~>COPY --from=builder /out /app
              """
          )
        );
    }

    @Test
    void identifiesStageReferenceByNumericIndex() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isTrue();
                assertThat(image.getImageName()).isEmpty();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS base
              FROM alpine
              COPY --from=0 /out /app
              """,
            """
              FROM alpine AS base
              FROM alpine
              ~~>COPY --from=0 /out /app
              """
          )
        );
    }

    @Test
    void bareNameThatIsNotAStageIsExternalImage() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isFalse();
                assertThat(image.getImageName()).contains("nginx");
                assertThat(image.getTag()).isEmpty();
                assertThat(image.isUnpinned()).isTrue();
                assertThat(image.getUnpinnedReason())
                  .contains(DockerImageReference.UnpinnedReason.IMPLICIT_LATEST);
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=nginx /usr/share/nginx /app
              """,
            """
              FROM alpine
              ~~>COPY --from=nginx /usr/share/nginx /app
              """
          )
        );
    }

    @Test
    void decomposesDigestInAddFrom() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.getImageName()).contains("alpine");
                assertThat(image.getTag()).isEmpty();
                assertThat(image.getDigest()).contains("sha256:abc123");
                assertThat(image.isDigestPinned()).isTrue();
                assertThat(image.isUnpinned()).isFalse();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              ADD --from=alpine@sha256:abc123 /out /app
              """,
            """
              FROM alpine
              ~~>ADD --from=alpine@sha256:abc123 /out /app
              """
          )
        );
    }

    @Test
    void preservesEnvironmentVariableValue() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().imageName("nginx").asVisitor((image, ctx) -> {
                assertThat(image.getFromValue()).contains("${IMG}");
                assertThat(image.isStageReference()).isFalse();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=${IMG} /out /app
              """,
            """
              FROM alpine
              ~~>COPY --from=${IMG} /out /app
              """
          )
        );
    }

    @Test
    void excludeStageReferencesMatchesOnlyExternalImages() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().excludeStageReferences().asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isFalse();
                assertThat(image.getImageName()).contains("nginx");
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS builder
              RUN make
              FROM alpine
              COPY --from=builder /out /app
              COPY --from=nginx:latest /usr/share/nginx /web
              """,
            """
              FROM alpine AS builder
              RUN make
              FROM alpine
              COPY --from=builder /out /app
              ~~>COPY --from=nginx:latest /usr/share/nginx /web
              """
          )
        );
    }

    @Test
    void doesNotMatchCopyWithoutFromFlag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) ->
              SearchResult.found(image.getTree()))
          )),
          docker(
            """
              FROM alpine
              COPY app /app
              """
          )
        );
    }

    @Test
    void registryPortIsNotMisSplitIntoTag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isFalse();
                assertThat(image.getImageName()).contains("registry.example.com:5000/app");
                assertThat(image.getTag()).isEmpty();
                assertThat(image.isUnpinned()).isTrue();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=registry.example.com:5000/app /out /app
              """,
            """
              FROM alpine
              ~~>COPY --from=registry.example.com:5000/app /out /app
              """
          )
        );
    }

    @Test
    void registryPortWithTag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.getImageName()).contains("registry.example.com:5000/app");
                assertThat(image.getTag()).contains("1.2");
                assertThat(image.getDigest()).isEmpty();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=registry.example.com:5000/app:1.2 /out /app
              """,
            """
              FROM alpine
              ~~>COPY --from=registry.example.com:5000/app:1.2 /out /app
              """
          )
        );
    }

    @Test
    void envVarOnlyReferenceIsConsideredPinned() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerCopyFrom.Matcher().asVisitor((image, ctx) -> {
                assertThat(image.isStageReference()).isFalse();
                assertThat(image.isUnpinned()).isFalse();
                assertThat(image.getUnpinnedReason()).isEmpty();
                // A pin recipe must leave the unresolved variable untouched
                return image.isUnpinned() ? image.withTag("3.19") : image.getTree();
            })
          )),
          docker(
            """
              FROM alpine
              COPY --from=${BUILDER_IMAGE} /out /app
              """
          )
        );
    }
}
