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
package org.openrewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class FromTest implements RewriteTest {

    @Test
    void simpleFrom() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getDigest()).isNull();
                assertThat(from.getAs()).isNull();
            })
          )
        );
    }

    @Test
    void fromWithPlatform() {
        rewriteRun(
          docker(
            """
              FROM --platform=linux/amd64 ubuntu:20.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(from.getFlags()).hasSize(1);
                assertThat(from.getFlags().getFirst().getName()).isEqualTo("platform");
                assertThat(((Docker.PlainText) from.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("linux/amd64");
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
            })
          )
        );
    }

    @Test
    void fromWithAs() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04 AS base
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.PlainText) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.PlainText) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getAs()).isNotNull();
                assertThat(((Docker.PlainText) from.getAs().getName().getContents().getFirst()).getText()).isEqualTo("base");
            })
          )
        );
    }

    @Test
    void multiStageFrom() {
        rewriteRun(
          docker(
            """
              FROM golang:1.20 AS builder
              RUN go build -o app .

              FROM alpine:latest
              RUN apk add --no-cache ca-certificates
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getStages())
              .satisfiesExactly(
                golang -> assertThat(((Docker.PlainText) golang.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("golang"),
                alpine -> assertThat(((Docker.PlainText) alpine.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("alpine")
              ))
          )
        );
    }

    @Test
    void fromWithPlatformEnvVar() {
        rewriteRun(
          docker(
            """
              FROM --platform=$BUILDPLATFORM node:18 AS builder
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(from.getFlags()).hasSize(1);
                assertThat(from.getFlags().getFirst().getName()).isEqualTo("platform");
                assertThat(from.getFlags().getFirst().getValue().getContents().getFirst())
                  .isInstanceOf(Docker.EnvironmentVariable.class);
                Docker.EnvironmentVariable envVar = (Docker.EnvironmentVariable) from.getFlags().getFirst().getValue().getContents().getFirst();
                assertThat(envVar.getName()).isEqualTo("BUILDPLATFORM");
            })
          )
        );
    }

    @Test
    void fromWithPlatformBracedEnvVar() {
        rewriteRun(
          docker(
            """
              FROM --platform=${TARGETPLATFORM} alpine:latest
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(from.getFlags()).hasSize(1);
                Docker.EnvironmentVariable envVar = (Docker.EnvironmentVariable) from.getFlags().getFirst().getValue().getContents().getFirst();
                assertThat(envVar.getName()).isEqualTo("TARGETPLATFORM");
                assertThat(envVar.isBraced()).isTrue();
            })
          )
        );
    }

    @Test
    void complexExpression() {
        rewriteRun(
          docker(
            """
              ARG VERSION=25
              FROM $REGISTRY/image:${VERSION}-suffix
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getLast().getFrom();

                // Check imageName contents
                List<Docker.ArgumentContent> imageNameContents = from.getImageName().getContents();
                assertThat(imageNameContents).hasSize(2);
                assertThat(imageNameContents.getFirst()).extracting(arg -> ((Docker.EnvironmentVariable) arg).getName()).isEqualTo("REGISTRY");
                assertThat(imageNameContents.get(1)).extracting(arg -> ((Docker.PlainText) arg).getText()).isEqualTo("/image");

                // Check tag contents
                assertThat(from.getTag()).isNotNull();
                List<Docker.ArgumentContent> tagContents = from.getTag().getContents();
                assertThat(tagContents).hasSize(2);
                assertThat(tagContents.getFirst()).extracting(arg -> ((Docker.EnvironmentVariable) arg).getName()).isEqualTo("VERSION");
                assertThat(tagContents.get(1)).extracting(arg -> ((Docker.PlainText) arg).getText()).isEqualTo("-suffix");

                // Check no digest
                assertThat(from.getDigest()).isNull();
            })
          )
        );
    }

    @Test
    void lowercaseInstructions() {
        rewriteRun(
          docker(
            """
              from ubuntu:20.04
              run apt-get update
              """,
            spec -> spec.afterRecipe(doc -> assertThat(doc.getStages().getFirst().getFrom().getKeyword()).isEqualTo("from"))
          )
        );
    }

    @Test
    void mixedCaseInstructions() {
        rewriteRun(
          docker(
            """
              From ubuntu:20.04 as builder
              Run apt-get update
              """
          )
        );
    }
}
