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

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class UseBuildKitCacheMountsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseBuildKitCacheMounts(null, null));
    }

    @DocumentExample
    @Test
    void maven() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              COPY . .
              RUN mvn -B package
              """,
            """
              FROM maven:3.9-eclipse-temurin-21
              COPY . .
              RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package
              """
          )
        );
    }

    @Test
    void mavenWrapper() {
        rewriteRun(
          docker(
            """
              FROM eclipse-temurin:21
              RUN ./mvnw -B package
              """,
            """
              FROM eclipse-temurin:21
              RUN --mount=type=cache,target=/root/.m2,sharing=locked ./mvnw -B package
              """
          )
        );
    }

    @Test
    void gradle() {
        rewriteRun(
          docker(
            """
              FROM eclipse-temurin:21
              RUN gradle build
              """,
            """
              FROM eclipse-temurin:21
              RUN --mount=type=cache,target=/root/.gradle,sharing=locked gradle build
              """
          )
        );
    }

    @Test
    void gradleImageKeepsItsCacheOutsideRootsHome() {
        rewriteRun(
          docker(
            """
              FROM gradle:8-jdk21
              RUN gradle build
              """
          )
        );
    }

    @Test
    void gradleWrapper() {
        rewriteRun(
          docker(
            """
              FROM eclipse-temurin:21
              RUN ./gradlew --no-daemon build
              """,
            """
              FROM eclipse-temurin:21
              RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew --no-daemon build
              """
          )
        );
    }

    @Test
    void npm() {
        rewriteRun(
          docker(
            """
              FROM node:22
              RUN npm ci
              """,
            """
              FROM node:22
              RUN --mount=type=cache,target=/root/.npm npm ci
              """
          )
        );
    }

    @Test
    void yarn() {
        rewriteRun(
          docker(
            """
              FROM node:22
              RUN yarn install --frozen-lockfile
              """,
            """
              FROM node:22
              RUN --mount=type=cache,target=/usr/local/share/.cache/yarn yarn install --frozen-lockfile
              """
          )
        );
    }

    @Test
    void pip() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              RUN pip install -r requirements.txt
              """,
            """
              FROM python:3.12
              RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
              """
          )
        );
    }

    @Test
    void go() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              RUN go mod download
              """,
            """
              FROM golang:1.22
              RUN --mount=type=cache,target=/root/.cache/go-build --mount=type=cache,target=/go/pkg/mod go mod download
              """
          )
        );
    }

    @Test
    void cargo() {
        rewriteRun(
          docker(
            """
              FROM rust:1.79
              RUN cargo build --release
              """,
            """
              FROM rust:1.79
              RUN --mount=type=cache,target=/usr/local/cargo/registry cargo build --release
              """
          )
        );
    }

    @Test
    void aptIsNotInTheDefaultSet() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """
          )
        );
    }

    @Test
    void aptAsksForTheCacheItCachesInto() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apt"), null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt/lists,sharing=locked rm -f /etc/apt/apt.conf.d/docker-clean && apt-get update && apt-get install -y curl
              """
          )
        );
    }

    @Test
    void aptCleanupDiscardsTheCache() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apt"), null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void aptCacheWithoutTheConfigurationThatDiscardsIt() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apt"), null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt/lists,sharing=locked apt-get update && apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt/lists,sharing=locked rm -f /etc/apt/apt.conf.d/docker-clean && apt-get update && apt-get install -y curl
              """
          )
        );
    }

    @Test
    void apk() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apk"), null)),
          docker(
            """
              FROM alpine:3.20
              RUN apk add curl
              """,
            """
              FROM alpine:3.20
              RUN --mount=type=cache,target=/var/cache/apk,sharing=locked apk add curl
              """
          )
        );
    }

    @Test
    void apkNoCacheKeepsNothingToCache() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apk"), null)),
          docker(
            """
              FROM alpine:3.20
              RUN apk add --no-cache curl
              """
          )
        );
    }

    @Test
    void pipInstallingIntoTheImage() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              RUN pip install --target=/app -r requirements.txt
              """
          )
        );
    }

    @Test
    void existingMountOfTheSameTarget() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN --mount=type=cache,target=/root/.m2 mvn -B package
              """
          )
        );
    }

    @Test
    void existingMountOfOneOfTwoTargets() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              RUN --mount=type=cache,target=/go/pkg/mod go mod download
              """,
            """
              FROM golang:1.22
              RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build go mod download
              """
          )
        );
    }

    @Test
    void unrelatedExistingFlagKeepsItsSpacing() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN --network=none mvn -B -o package
              """,
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN --network=none --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -o package
              """
          )
        );
    }

    @Test
    void execForm() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN ["mvn", "-B", "package"]
              """,
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN --mount=type=cache,target=/root/.m2,sharing=locked ["mvn", "-B", "package"]
              """
          )
        );
    }

    @Test
    void heredocFormIsLeftAlone() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN <<EOF
              mvn -B package
              EOF
              """
          )
        );
    }

    @Test
    void chainedCommand() {
        rewriteRun(
          docker(
            """
              FROM node:22
              RUN npm ci && npm run build
              """,
            """
              FROM node:22
              RUN --mount=type=cache,target=/root/.npm npm ci && npm run build
              """
          )
        );
    }

    @Test
    void lineContinuation() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              RUN python -V && \\
                  pip install -r requirements.txt
              """,
            """
              FROM python:3.12
              RUN --mount=type=cache,target=/root/.cache/pip python -V && \\
                  pip install -r requirements.txt
              """
          )
        );
    }

    @Test
    void onlyTheBuildStageOfAMultiStageBuild() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21 AS build
              COPY . .
              RUN mvn -B package

              FROM eclipse-temurin:21-jre
              COPY --from=build /target/app.jar /app.jar
              RUN echo "mvn is not run here"
              ENTRYPOINT ["java", "-jar", "/app.jar"]
              """,
            """
              FROM maven:3.9-eclipse-temurin-21 AS build
              COPY . .
              RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package

              FROM eclipse-temurin:21-jre
              COPY --from=build /target/app.jar /app.jar
              RUN echo "mvn is not run here"
              ENTRYPOINT ["java", "-jar", "/app.jar"]
              """
          )
        );
    }

    @Test
    void nonRootUserHasAnotherHomeDirectory() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              USER build
              RUN mvn -B package
              """
          )
        );
    }

    @Test
    void rootUserRestoredBeforeTheBuild() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              USER build
              RUN echo building
              USER root
              RUN mvn -B package
              """,
            """
              FROM maven:3.9-eclipse-temurin-21
              USER build
              RUN echo building
              USER root
              RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package
              """
          )
        );
    }

    @Test
    void nonRootUserOfAnEarlierStage() {
        rewriteRun(
          docker(
            """
              FROM eclipse-temurin:21 AS tools
              USER build

              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """,
            """
              FROM eclipse-temurin:21 AS tools
              USER build

              FROM maven:3.9-eclipse-temurin-21
              RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package
              """
          )
        );
    }

    @Test
    void frontendOlderThanCacheMounts() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1.0
              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """
          )
        );
    }

    @Test
    void frontendNewEnoughForCacheMounts() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1
              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """,
            """
              # syntax=docker/dockerfile:1
              FROM maven:3.9-eclipse-temurin-21
              RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package
              """
          )
        );
    }

    @Test
    void packageManagersSelectsWhatIsMounted() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("npm"), null)),
          docker(
            """
              FROM node:22
              RUN npm ci
              RUN pip install -r requirements.txt
              """,
            """
              FROM node:22
              RUN --mount=type=cache,target=/root/.npm npm ci
              RUN pip install -r requirements.txt
              """
          )
        );
    }

    @Test
    void sharingOverridesTheModeOfEveryMount() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(null, "private")),
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """,
            """
              FROM maven:3.9-eclipse-temurin-21
              RUN --mount=type=cache,target=/root/.m2,sharing=private mvn -B package
              """
          )
        );
    }

    @Test
    void aPackageManagerNamedInPassingIsNotRun() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN echo "run mvn package to build"
              """
          )
        );
    }

    @Test
    void aptWithoutTheUpdateThatFillsTheCache() {
        rewriteRun(
          spec -> spec.recipe(new UseBuildKitCacheMounts(singletonList("apt"), null)),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              RUN apt-get install -y curl
              """
          )
        );
    }

    @Test
    void separatorInsideAQuotedStringSeparatesNothing() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN echo "first && mvn package"
              """
          )
        );
    }

    @Test
    void frontendOlderThanCacheMountsPinnedByDigest() {
        rewriteRun(
          docker(
            """
              # syntax=docker/dockerfile:1.0@sha256:1234567890abcdef
              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """
          )
        );
    }

    @Test
    void escapeDirectiveBeforeTheSyntaxDirective() {
        rewriteRun(
          docker(
            """
              # escape=`
              # syntax=docker/dockerfile:1.0
              FROM maven:3.9-eclipse-temurin-21
              RUN mvn -B package
              """
          )
        );
    }

    @Test
    void pipToldNotToCache() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              RUN pip install --no-cache-dir -r requirements.txt
              """
          )
        );
    }

    @Test
    void npmClearingTheCacheItJustFilled() {
        rewriteRun(
          docker(
            """
              FROM node:22
              RUN npm ci && npm cache clean --force
              """
          )
        );
    }

    @Test
    void yarnClearingTheCacheItJustFilled() {
        rewriteRun(
          docker(
            """
              FROM node:22
              RUN yarn install --frozen-lockfile && yarn cache clean
              """
          )
        );
    }

    @Test
    void goCleaningTheModuleCache() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              RUN go mod download && go clean -modcache
              """
          )
        );
    }

    @Test
    void commandRemovingOneOfTwoCacheDirectories() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              RUN go mod download && rm -rf /go/pkg/mod
              """,
            """
              FROM golang:1.22
              RUN --mount=type=cache,target=/root/.cache/go-build go mod download && rm -rf /go/pkg/mod
              """
          )
        );
    }

    @Test
    void environmentMovingTheCache() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              ENV GOPATH=/gopath
              RUN go mod download
              """,
            """
              FROM golang:1.22
              ENV GOPATH=/gopath
              RUN --mount=type=cache,target=/root/.cache/go-build go mod download
              """
          )
        );
    }

    @Test
    void environmentRestatingTheDefaultCacheLocation() {
        rewriteRun(
          docker(
            """
              FROM rust:1.79
              ENV CARGO_HOME=/usr/local/cargo
              RUN cargo build --release
              """,
            """
              FROM rust:1.79
              ENV CARGO_HOME=/usr/local/cargo
              RUN --mount=type=cache,target=/usr/local/cargo/registry cargo build --release
              """
          )
        );
    }

    @Test
    void homeIsSomewhereElse() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              ENV HOME=/home/app
              RUN pip install -r requirements.txt
              """
          )
        );
    }

    @Test
    void mountWouldHideACopiedSettingsFile() {
        rewriteRun(
          docker(
            """
              FROM maven:3.9-eclipse-temurin-21
              COPY settings.xml /root/.m2/settings.xml
              RUN mvn -B package
              """
          )
        );
    }

    @Test
    void pipRunAsAPythonModule() {
        rewriteRun(
          docker(
            """
              FROM python:3.12
              RUN python3 -m pip install -r requirements.txt
              """,
            """
              FROM python:3.12
              RUN --mount=type=cache,target=/root/.cache/pip python3 -m pip install -r requirements.txt
              """
          )
        );
    }

    @Test
    void goInstall() {
        rewriteRun(
          docker(
            """
              FROM golang:1.22
              RUN go install github.com/example/tool@latest
              """,
            """
              FROM golang:1.22
              RUN --mount=type=cache,target=/root/.cache/go-build --mount=type=cache,target=/go/pkg/mod go install github.com/example/tool@latest
              """
          )
        );
    }

    @Test
    void cargoInstall() {
        rewriteRun(
          docker(
            """
              FROM rust:1.79
              RUN cargo install cargo-watch
              """,
            """
              FROM rust:1.79
              RUN --mount=type=cache,target=/usr/local/cargo/registry cargo install cargo-watch
              """
          )
        );
    }

    @Test
    void unknownPackageManagerIsRejected() {
        assertThat(new UseBuildKitCacheMounts(List.of("bower"), null).validate().isValid()).isFalse();
    }

    @Test
    void unknownSharingModeIsRejected() {
        assertThat(new UseBuildKitCacheMounts(null, "exclusive").validate().isValid()).isFalse();
    }
}
