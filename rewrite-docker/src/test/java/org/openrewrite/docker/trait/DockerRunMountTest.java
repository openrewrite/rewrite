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

class DockerRunMountTest implements RewriteTest {

    @DocumentExample
    @Test
    void upgradesMountFromTag() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().imageName("composer").tag("2.5.5").asVisitor((mount, ctx) ->
              mount.withTag("2.8.3"))
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=bind,from=composer:2.5.5,source=/usr/bin/composer,target=/usr/bin/composer composer install
              """,
            """
              FROM alpine
              RUN --mount=type=bind,from=composer:2.8.3,source=/usr/bin/composer,target=/usr/bin/composer composer install
              """
          )
        );
    }

    @Test
    void readsMountOptions() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getType()).isEqualTo("bind");
                assertThat(mount.getSource()).contains("/usr/bin/composer");
                assertThat(mount.getTarget()).contains("/opt/bin/composer");
                assertThat(mount.getOption("from")).contains("composer:2.5.5");
                assertThat(mount.isStageReference()).isFalse();
                assertThat(mount.getImageName()).contains("composer");
                assertThat(mount.getTag()).contains("2.5.5");
                assertThat(mount.getDigest()).isEmpty();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=bind,from=composer:2.5.5,source=/usr/bin/composer,target=/opt/bin/composer composer install
              """,
            """
              FROM alpine
              RUN ~~>--mount=type=bind,from=composer:2.5.5,source=/usr/bin/composer,target=/opt/bin/composer composer install
              """
          )
        );
    }

    @Test
    void typeDefaultsToBindAndAliasesAreRead() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getType()).isEqualTo("bind");
                assertThat(mount.getSource()).contains("/out");
                assertThat(mount.getTarget()).contains("/app");
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=src=/out,dst=/app,from=alpine:3.19 ls /app
              """,
            """
              FROM alpine
              RUN ~~>--mount=src=/out,dst=/app,from=alpine:3.19 ls /app
              """
          )
        );
    }

    @Test
    void aBooleanOptionIsCarriedWithoutAValue() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getType()).isEqualTo("secret");
                assertThat(mount.getOption("ro")).contains("");
                assertThat(mount.getOption("required")).contains("");
                assertThat(mount.getOption("rw")).isEmpty();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=secret,id=aws,target=/root/.aws/creds,required,ro cat /root/.aws/creds
              """,
            """
              FROM alpine
              RUN ~~>--mount=type=secret,id=aws,target=/root/.aws/creds,required,ro cat /root/.aws/creds
              """
          )
        );
    }

    @Test
    void anOptionIsReadWithoutRegardToCase() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getType()).isEqualTo("cache");
                assertThat(mount.getTarget()).contains("/c");
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=TYPE=Cache,TARGET=/c go build
              """,
            """
              FROM alpine
              RUN ~~>--mount=TYPE=Cache,TARGET=/c go build
              """
          )
        );
    }

    @Test
    void identifiesStageReferenceByName() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.isStageReference()).isTrue();
                assertThat(mount.getImageName()).isEmpty();
                assertThat(mount.getTag()).isEmpty();
                assertThat(mount.isUnpinned()).isFalse();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS upstream
              RUN make
              FROM alpine
              RUN --mount=type=bind,from=upstream,source=/usr/src,target=/usr/local/src,rw ls /usr/local/src
              """,
            """
              FROM alpine AS upstream
              RUN make
              FROM alpine
              RUN ~~>--mount=type=bind,from=upstream,source=/usr/src,target=/usr/local/src,rw ls /usr/local/src
              """
          )
        );
    }

    @Test
    void stageNameIsMatchedWithoutRegardToCase() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.isStageReference()).isTrue();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS Builder
              FROM alpine
              RUN --mount=type=bind,from=builder,target=/out ls /out
              """,
            """
              FROM alpine AS Builder
              FROM alpine
              RUN ~~>--mount=type=bind,from=builder,target=/out ls /out
              """
          )
        );
    }

    @Test
    void numberIsAnImageNameRatherThanAStageIndex() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.isStageReference()).isFalse();
                assertThat(mount.getImageName()).contains("0");
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine AS base
              FROM alpine
              RUN --mount=type=bind,from=0,target=/out ls /out
              """,
            """
              FROM alpine AS base
              FROM alpine
              RUN ~~>--mount=type=bind,from=0,target=/out ls /out
              """
          )
        );
    }

    @Test
    void decomposesRegistryPortAndDigest() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getImageName()).contains("registry:5000/php/pie");
                assertThat(mount.getTag()).contains("bin");
                assertThat(mount.getDigest()).contains("sha256:abc123");
                assertThat(mount.isDigestPinned()).isTrue();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=bind,from=registry:5000/php/pie:bin@sha256:abc123,source=/,target=/opt/bin pie install
              """,
            """
              FROM alpine
              RUN ~~>--mount=type=bind,from=registry:5000/php/pie:bin@sha256:abc123,source=/,target=/opt/bin pie install
              """
          )
        );
    }

    @Test
    void everyMountOfOneRunIsItsOwnReference() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().excludeStageReferences().asVisitor((mount, ctx) ->
              mount.getTag().filter("2.5.5"::equals).isPresent() ? mount.withTag("2.8.3") : mount.getTree())
          )),
          docker(
            """
              FROM alpine AS upstream
              FROM alpine
              RUN --mount=type=bind,from=upstream,target=/src --mount=type=bind,from=composer:2.5.5,target=/opt \\
                  --mount=type=cache,target=/cache composer install
              """,
            """
              FROM alpine AS upstream
              FROM alpine
              RUN --mount=type=bind,from=upstream,target=/src --mount=type=bind,from=composer:2.8.3,target=/opt \\
                  --mount=type=cache,target=/cache composer install
              """
          )
        );
    }

    @Test
    void readsFromWhereverItStandsInTheOptionList() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().imageName("composer").asVisitor((mount, ctx) -> {
                assertThat(mount.getType()).isEqualTo("cache");
                assertThat(mount.getImageName()).contains("composer");
                return mount.isUnpinned() ? mount.withImageReference("composer:2.8.3") : mount.getTree();
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=cache,target=/ppm,from=composer,source=/ppm ppm install
              """,
            """
              FROM alpine
              RUN --mount=type=cache,target=/ppm,from=composer:2.8.3,source=/ppm ppm install
              """
          )
        );
    }

    @Test
    void mountWithoutFromTakesFilesFromTheBuildContext() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().onlyWithFrom().asVisitor((mount, ctx) ->
              SearchResult.found(mount.getTree()))
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=cache,target=/var/cache/apk apk add curl
              """
          )
        );
    }

    @Test
    void anEmptyFromIsNotAnImage() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getImageName()).isEmpty();
                assertThat(mount.isStageReference()).isFalse();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=bind,from='',source='',target='',rw ls /
              """,
            """
              FROM alpine
              RUN ~~>--mount=type=bind,from='',source='',target='',rw ls /
              """
          )
        );
    }

    @Test
    void aFromHoldingAVariableIsNotAnImage() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) -> {
                assertThat(mount.getOption("from")).contains("${IMG}");
                assertThat(mount.getImageName()).isEmpty();
                return SearchResult.found(mount.getTree());
            })
          )),
          docker(
            """
              FROM alpine
              RUN --mount=type=bind,from=${IMG},target=/out ls /out
              """,
            """
              FROM alpine
              RUN ~~>--mount=type=bind,from=${IMG},target=/out ls /out
              """
          )
        );
    }

    @Test
    void flagsOfOtherInstructionsAreNotMounts() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new DockerRunMount.Matcher().asVisitor((mount, ctx) ->
              SearchResult.found(mount.getTree()))
          )),
          docker(
            """
              FROM --platform=linux/amd64 alpine AS builder
              COPY --from=nginx:1.25 --chown=1000:1000 /usr/share/nginx /app
              RUN --network=none ls /app
              """
          )
        );
    }
}
