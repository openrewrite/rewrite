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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.test.TypeValidation.all;

class RemoveUnusedStagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnusedStages());
    }

    @DocumentExample
    @Test
    void removeAStageNothingReaches() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9 AS build
              RUN mvn package

              FROM golang:1.22 AS tools
              RUN go build ./cmd/lint

              FROM eclipse-temurin:21-jre
              COPY --from=build /target/app.jar /app.jar
              """,
            """
              FROM maven:3.9 AS build
              RUN mvn package

              FROM eclipse-temurin:21-jre
              COPY --from=build /target/app.jar /app.jar
              """
          )
        );
    }

    @Test
    void removeAChainOfStagesOnlyTheRemovedStagesReached() {
        rewriteRun(
          docker(
            """
              FROM alpine AS a
              RUN a

              FROM a AS b
              RUN b

              FROM b AS c
              COPY --from=a /x /y

              FROM alpine
              RUN done
              """,
            """
              FROM alpine
              RUN done
              """
          )
        );
    }

    @Test
    void keepAStageOnlyACopyFromReaches() {
        rewriteRun(
          docker(
            """
              FROM node:22 AS assets
              RUN npm run build

              FROM nginx:alpine
              COPY --from=assets /dist /usr/share/nginx/html
              """
          )
        );
    }

    @Test
    void keepAStageOnlyABindMountReaches() {
        rewriteRun(
          docker(
            """
              FROM alpine AS certs
              RUN update-ca-certificates

              FROM alpine
              RUN --mount=type=bind,from=certs,source=/etc/ssl,target=/ssl cp -r /ssl /etc/ssl
              """
          )
        );
    }

    @Test
    void keepAStageALaterFromExtends() {
        rewriteRun(
          docker(
            """
              FROM alpine AS base
              RUN apk add curl

              FROM base
              RUN curl https://example.com
              """
          )
        );
    }

    @Test
    void removeAnUnnamedStage() {
        rewriteRun(
          docker(
            """
              FROM alpine
              RUN echo scratch work

              FROM busybox
              RUN echo real work
              """,
            """
              FROM busybox
              RUN echo real work
              """
          )
        );
    }

    @Test
    void doNotTouchAFileThatNamesAStageByIndex() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM node:22 AS assets
              RUN npm run build

              FROM nginx:alpine
              COPY --from=1 /dist /usr/share/nginx/html
              """
          )
        );
    }

    @Test
    void doNotTouchAFileThatNamesAStageWithAVariable() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM node:22 AS assets
              RUN npm run build

              FROM nginx:alpine
              COPY --from=$STAGE /dist /usr/share/nginx/html
              """
          )
        );
    }

    @Test
    void doNotTouchAFileWhoseFromCouldResolveToAStage() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM node:22 AS assets
              RUN npm run build

              FROM $BASE
              RUN echo done
              """
          )
        );
    }

    @Test
    void aRegistryQualifiedVariableIsNotAStageReference() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM ${REGISTRY}/nginx
              RUN echo done
              """,
            """
              FROM ${REGISTRY}/nginx
              RUN echo done
              """
          )
        );
    }

    @Test
    void neverRemoveTheFinalStage() {
        rewriteRun(
          docker(
            """
              FROM alpine AS base
              RUN echo base

              FROM base AS shipped
              RUN echo last
              """
          )
        );
    }

    /// The Docker parser drops what follows an `ARG` naming more than one build argument into the space at the end
    /// of the file, so the `COPY --from=build` below is not in the tree at all. Where the parser leaves text it could
    /// not place, nothing is removed. Fix that parse and this file becomes an ordinary no-op instead.
    @Test
    void leaveAFileWithTextTheParserCouldNotPlaceAlone() {
        rewriteRun(
          spec -> spec.executionContext(new InMemoryExecutionContext())
            .typeValidationOptions(all().allowNonWhitespaceInWhitespace(true)),
          docker(
            """
              FROM golang:1.24 AS build
              RUN go build

              FROM scratch
              ARG TARGETOS TARGETARCH
              COPY --from=build /out/app /app
              """
          )
        );
    }

    @Test
    void keepAStageADockerBakeFileBuilds() {
        rewriteRun(
          text(
            """
              target "lint" {
                dockerfile = "Dockerfile"
                target = "lint"
                output = ["type=cacheonly"]
              }
              """,
            spec -> spec.path("docker-bake.hcl")
          ),
          docker(
            """
              FROM golangci/golangci-lint AS lint
              RUN golangci-lint run

              FROM alpine
              RUN echo done
              """
          )
        );
    }

    @Test
    void keepAStageAComposeFileBuilds() {
        rewriteRun(
          text(
            """
              services:
                app:
                  build:
                    context: .
                    target: dev
              """,
            spec -> spec.path("docker-compose.yml")
          ),
          docker(
            """
              FROM node:22 AS dev
              RUN npm install

              FROM node:22-slim
              RUN echo done
              """
          )
        );
    }

    @Test
    void keepAStageACiWorkflowBuilds() {
        rewriteRun(
          text(
            """
              jobs:
                test:
                  steps:
                    - run: docker build --target ci -t app:ci .
              """,
            spec -> spec.path(".github/workflows/build.yml")
          ),
          docker(
            """
              FROM python:3.13 AS ci
              RUN pytest

              FROM python:3.13-slim
              RUN echo done
              """
          )
        );
    }

    @Test
    void keepAStageTheDockerfileItselfDocumentsBuilding() {
        rewriteRun(
          docker(
            """
              # Build the shipping image with:
              #   docker build --target production -t app .
              # and the one carrying a shell with:
              #   docker build --target debug -t app:debug .
              FROM alpine AS production
              RUN echo production

              FROM busybox AS debug
              RUN echo debug
              """
          )
        );
    }

    @Test
    void keepWhatAStageABuildTargetsReaches() {
        rewriteRun(
          text(
            """
              docker build --target test .
              """,
            spec -> spec.path("hack/test.sh")
          ),
          docker(
            """
              FROM golang:1.24 AS base
              RUN go mod download

              FROM base AS test
              RUN go test ./...

              FROM alpine
              RUN echo done
              """
          )
        );
    }

    @Test
    void ignoreATargetNamedByAFileThatDoesNotDriveBuilds() {
        rewriteRun(
          text(
            """
              Run `docker build --target lint .` to lint.
              """,
            spec -> spec.path("CONTRIBUTING.md")
          ),
          docker(
            """
              FROM golangci/golangci-lint AS lint
              RUN golangci-lint run

              FROM alpine
              RUN echo done
              """,
            """
              FROM alpine
              RUN echo done
              """
          )
        );
    }

    @Test
    void leaveASingleStageFileAlone() {
        rewriteRun(
          docker(
            """
              FROM alpine
              RUN echo hi
              """
          )
        );
    }

    @Test
    void takeTheCommentsOfARemovedStageWithIt() {
        rewriteRun(
          docker(
            """
              FROM alpine AS keep
              RUN echo keep

              # This stage lints, and nothing depends on it.
              # It runs only in CI.
              FROM golangci/golangci-lint AS lint
              RUN golangci-lint run

              FROM keep
              RUN echo done
              """,
            """
              FROM alpine AS keep
              RUN echo keep

              FROM keep
              RUN echo done
              """
          )
        );
    }

    @Test
    void removeTheFirstStage() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM busybox
              RUN echo real work
              """,
            """
              FROM busybox
              RUN echo real work
              """
          )
        );
    }

    @Test
    void removeTheFirstStageAfterAGlobalArg() {
        rewriteRun(
          docker(
            """
              ARG VERSION=22
              FROM alpine AS unused
              RUN echo nothing

              FROM node:${VERSION}
              RUN echo real work
              """,
            """
              ARG VERSION=22
              FROM node:${VERSION}
              RUN echo real work
              """
          )
        );
    }

    @Test
    void removeTheFirstStageUnderAFileComment() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1

              FROM alpine AS unused
              RUN echo nothing

              # The image we ship.
              FROM busybox
              RUN echo real work
              """,
            """
              # syntax=docker/dockerfile:1

              # The image we ship.
              FROM busybox
              RUN echo real work
              """
          )
        );
    }

    @Test
    void matchStageNamesIgnoringCase() {
        rewriteRun(
          docker(
            """
              FROM alpine AS builder
              RUN echo build

              FROM busybox
              COPY --from=BUILDER /a /b
              """
          )
        );
    }

    @Test
    void anExternalImageInACopyFromIsNotAStageReference() {
        rewriteRun(
          docker(
            """
              FROM alpine AS unused
              RUN echo nothing

              FROM busybox
              COPY --from=nginx:alpine /etc/nginx /etc/nginx
              """,
            """
              FROM busybox
              COPY --from=nginx:alpine /etc/nginx /etc/nginx
              """
          )
        );
    }

    @Test
    void removeSeveralStagesAtOnce() {
        rewriteRun(
          docker(
            """
              FROM alpine AS one
              RUN echo one

              FROM alpine AS two
              RUN echo two

              FROM alpine AS three
              RUN echo three

              FROM two
              COPY --from=three /a /b
              """,
            """
              FROM alpine AS two
              RUN echo two

              FROM alpine AS three
              RUN echo three

              FROM two
              COPY --from=three /a /b
              """
          )
        );
    }
}
