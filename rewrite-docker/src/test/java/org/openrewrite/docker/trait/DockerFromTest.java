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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class DockerFromTest implements RewriteTest {

    @DocumentExample
    @Test
    void matchesSimpleImage() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerFrom.Matcher().imageName("ubuntu").asVisitor((image, ctx) -> {
                assertThat(image.getImageName()).isEqualTo("ubuntu");
                assertThat(image.getTag()).isEqualTo("20.04");
                assertThat(image.getDigest()).isNull();
                assertThat(image.getPlatform()).isNull();
                assertThat(image.getStageName()).isNull();
                assertThat(image.isScratch()).isFalse();
                assertThat(image.isUnpinned()).isFalse();
                return SearchResult.found(image.getTree());
            })
          )),
          docker(
            """
              FROM ubuntu:20.04
              RUN apt-get update
              """,
            """
              ~~>FROM ubuntu:20.04
              RUN apt-get update
              """
          )
        );
    }

    @Nested
    class Accessors implements RewriteTest {

        @Test
        void extractsAllComponents() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.getImageName()).isEqualTo("my.registry.com/library/ubuntu");
                    assertThat(image.getTag()).isEqualTo("22.04");
                    assertThat(image.getDigest()).isNull();
                    assertThat(image.getPlatform()).isEqualTo("linux/amd64");
                    assertThat(image.getStageName()).isEqualTo("builder");
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM --platform=linux/amd64 my.registry.com/library/ubuntu:22.04 AS builder
                  RUN apt-get update
                  """,
                """
                  ~~>FROM --platform=linux/amd64 my.registry.com/library/ubuntu:22.04 AS builder
                  RUN apt-get update
                  """
              )
            );
        }

        @Test
        void extractsDigest() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.getImageName()).isEqualTo("ubuntu");
                    assertThat(image.getTag()).isNull();
                    assertThat(image.getDigest()).isEqualTo("sha256:abc123def456");
                    assertThat(image.isUnpinned()).isFalse();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM ubuntu@sha256:abc123def456
                  """,
                """
                  ~~>FROM ubuntu@sha256:abc123def456
                  """
              )
            );
        }

        @Test
        void extractsTagAndDigest() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.getImageName()).isEqualTo("ubuntu");
                    assertThat(image.getTag()).isEqualTo("20.04");
                    assertThat(image.getDigest()).isEqualTo("sha256:abc123");
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM ubuntu:20.04@sha256:abc123
                  """,
                """
                  ~~>FROM ubuntu:20.04@sha256:abc123
                  """
              )
            );
        }

        @Test
        void identifiesScratchImage() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.isScratch()).isTrue();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM scratch
                  COPY app /app
                  """,
                """
                  ~~>FROM scratch
                  COPY app /app
                  """
              )
            );
        }

        @Test
        void identifiesUnpinnedImageWithLatestTag() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.isUnpinned()).isTrue();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM alpine:latest
                  """,
                """
                  ~~>FROM alpine:latest
                  """
              )
            );
        }

        @Test
        void identifiesUnpinnedImageWithNoTag() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.isUnpinned()).isTrue();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM alpine
                  """,
                """
                  ~~>FROM alpine
                  """
              )
            );
        }

        @Test
        void pinnedByDigestIsNotUnpinned() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.isUnpinned()).isFalse();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM alpine@sha256:abc123
                  """,
                """
                  ~~>FROM alpine@sha256:abc123
                  """
              )
            );
        }

        @Test
        void detectsQuoteStyle() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.getQuoteStyle()).isNotNull();
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM "ubuntu:20.04"
                  """,
                """
                  ~~>FROM "ubuntu:20.04"
                  """
              )
            );
        }
    }

    @Nested
    class EnvironmentVariables implements RewriteTest {

        @Test
        void preservesUnbracedVariableForm() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) -> {
                    assertThat(image.getTag()).isEqualTo("$TAG");
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM ubuntu:$TAG
                  """,
                """
                  ~~>FROM ubuntu:$TAG
                  """
              )
            );
        }
    }

    @Nested
    class Matching implements RewriteTest {

        @Test
        void matchesWithWildcard() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().imageName("ubuntu").tag("*").asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM ubuntu:20.04
                  FROM alpine:latest
                  """,
                """
                  ~~>FROM ubuntu:20.04
                  FROM alpine:latest
                  """
              )
            );
        }

        @Test
        void matchesPlatform() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().platform("linux/amd64").asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM --platform=linux/amd64 ubuntu:20.04
                  FROM --platform=linux/arm64 ubuntu:20.04
                  FROM ubuntu:20.04
                  """,
                """
                  ~~>FROM --platform=linux/amd64 ubuntu:20.04
                  FROM --platform=linux/arm64 ubuntu:20.04
                  FROM ubuntu:20.04
                  """
              )
            );
        }

        @Test
        void matchesPlatformWithWildcard() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().platform("linux/*").asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM --platform=linux/amd64 ubuntu:20.04
                  FROM --platform=linux/arm64 ubuntu:20.04
                  FROM --platform=windows/amd64 ubuntu:20.04
                  """,
                """
                  ~~>FROM --platform=linux/amd64 ubuntu:20.04
                  ~~>FROM --platform=linux/arm64 ubuntu:20.04
                  FROM --platform=windows/amd64 ubuntu:20.04
                  """
              )
            );
        }
    }

    @Nested
    class Filters implements RewriteTest {

        @Test
        void excludesScratch() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().excludeScratch().asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM scratch
                  COPY app /app

                  FROM alpine:latest
                  RUN apk update
                  """,
                """
                  FROM scratch
                  COPY app /app

                  ~~>FROM alpine:latest
                  RUN apk update
                  """
              )
            );
        }

        @Test
        void onlyUnpinnedImages() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().onlyUnpinned().asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM alpine:latest
                  FROM ubuntu:20.04
                  FROM nginx
                  FROM debian@sha256:abc123
                  """,
                """
                  ~~>FROM alpine:latest
                  FROM ubuntu:20.04
                  ~~>FROM nginx
                  FROM debian@sha256:abc123
                  """
              )
            );
        }

        @Test
        void combinedFilters() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher()
                  .imageName("ubuntu")
                  .excludeScratch()
                  .asVisitor((image, ctx) -> SearchResult.found(image.getTree()))
              )),
              docker(
                """
                  FROM ubuntu:20.04
                  FROM ubuntu:22.04
                  FROM alpine:latest
                  """,
                """
                  ~~>FROM ubuntu:20.04
                  ~~>FROM ubuntu:22.04
                  FROM alpine:latest
                  """
              )
            );
        }
    }

    @Nested
    class MultiStage implements RewriteTest {

        @Test
        void matchesSpecificStages() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().imageName("golang").asVisitor((image, ctx) -> {
                    assertThat(image.getStageName()).isEqualTo("builder");
                    return SearchResult.found(image.getTree());
                })
              )),
              docker(
                """
                  FROM golang:1.21 AS builder
                  RUN go build -o app .

                  FROM alpine:latest
                  COPY --from=builder /app /app
                  """,
                """
                  ~~>FROM golang:1.21 AS builder
                  RUN go build -o app .

                  FROM alpine:latest
                  COPY --from=builder /app /app
                  """
              )
            );
        }

        @Test
        void matchesAllStages() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM golang:1.21 AS builder
                  FROM alpine:latest AS runtime
                  FROM scratch
                  """,
                """
                  ~~>FROM golang:1.21 AS builder
                  ~~>FROM alpine:latest AS runtime
                  ~~>FROM scratch
                  """
              )
            );
        }
    }

    @Nested
    class DigestMatching implements RewriteTest {

        @Test
        void matchesDigestPattern() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() ->
                new DockerFrom.Matcher().digest("sha256:*").asVisitor((image, ctx) ->
                  SearchResult.found(image.getTree())
                )
              )),
              docker(
                """
                  FROM ubuntu@sha256:abc123
                  FROM alpine:latest
                  """,
                """
                  ~~>FROM ubuntu@sha256:abc123
                  FROM alpine:latest
                  """
              )
            );
        }
    }
}
