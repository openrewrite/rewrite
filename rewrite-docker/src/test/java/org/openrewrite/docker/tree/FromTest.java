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
import org.openrewrite.docker.internal.ArgumentContents;
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
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
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
                assertThat(((Docker.Literal) from.getFlags().getFirst().getValue().getContents().getFirst()).getText()).isEqualTo("linux/amd64");
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
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
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("20.04");
                assertThat(from.getAs()).isNotNull();
                assertThat(from.getAs().getName().getText()).isEqualTo("base");
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
                golang -> assertThat(((Docker.Literal) golang.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("golang"),
                alpine -> assertThat(((Docker.Literal) alpine.getFrom().getImageName().getContents().getFirst()).getText()).isEqualTo("alpine")
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
                var envVar = (Docker.EnvironmentVariable) from.getFlags().getFirst().getValue().getContents().getFirst();
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
                var envVar = (Docker.EnvironmentVariable) from.getFlags().getFirst().getValue().getContents().getFirst();
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
                assertThat(imageNameContents.get(1)).extracting(arg -> ((Docker.Literal) arg).getText()).isEqualTo("/image");

                // Check tag contents
                assertThat(from.getTag()).isNotNull();
                List<Docker.ArgumentContent> tagContents = from.getTag().getContents();
                assertThat(tagContents).hasSize(2);
                assertThat(tagContents.getFirst()).extracting(arg -> ((Docker.EnvironmentVariable) arg).getName()).isEqualTo("VERSION");
                assertThat(tagContents.get(1)).extracting(arg -> ((Docker.Literal) arg).getText()).isEqualTo("-suffix");

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

    @Test
    void fromWithTagAndDigest() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04@sha256:abc123def456
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(from.getTag()).isNotNull();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
                assertThat(from.getDigest()).isNotNull();
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123def456");
            })
          )
        );
    }

    @Test
    void fromWithDigestOnly() {
        rewriteRun(
          docker(
            """
              FROM ubuntu@sha256:abc123def456
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(from.getTag()).isNull();
                assertThat(from.getDigest()).isNotNull();
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123def456");
            })
          )
        );
    }

    @Test
    void fromWithTagAndDigestAndAs() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04@sha256:abc123 AS base
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(from.getTag()).isNotNull();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
                assertThat(from.getDigest()).isNotNull();
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123");
                assertThat(from.getAs()).isNotNull();
                assertThat(from.getAs().getName().getText()).isEqualTo("base");
            })
          )
        );
    }

    @Test
    void fromWithRegistryTagAndDigest() {
        rewriteRun(
          docker(
            """
              FROM my.registry.com/ubuntu:22.04@sha256:abc123
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("my.registry.com/ubuntu");
                assertThat(from.getTag()).isNotNull();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
                assertThat(from.getDigest()).isNotNull();
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123");
            })
          )
        );
    }

    @Test
    void fromWithEnvVarTagAndDigest() {
        rewriteRun(
          docker(
            """
              ARG TAG=22.04
              FROM ubuntu:${TAG}@sha256:abc123
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getLast().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");

                // Tag should contain the env var
                assertThat(from.getTag()).isNotNull();
                assertThat(from.getTag().getContents().getFirst()).isInstanceOf(Docker.EnvironmentVariable.class);
                assertThat(((Docker.EnvironmentVariable) from.getTag().getContents().getFirst()).getName()).isEqualTo("TAG");

                assertThat(from.getDigest()).isNotNull();
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123");
            })
          )
        );
    }

    @Test
    void registryPortIsNotATag() {
        rewriteRun(
          docker(
            """
              FROM localhost:5000/my/app
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("localhost:5000/my/app");
                assertThat(from.getTag()).isNull();
            })
          )
        );
    }

    @Test
    void registryPortWithTagAndDigest() {
        rewriteRun(
          docker(
            """
              FROM localhost:5000/my/app:1.2.3@sha256:abc123
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("localhost:5000/my/app");
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("1.2.3");
                assertThat(((Docker.Literal) from.getDigest().getContents().getFirst()).getText()).isEqualTo("sha256:abc123");
            })
          )
        );
    }

    @Test
    void quotedImageReferenceKeepsItsColon() {
        rewriteRun(
          docker(
            """
              FROM "ubuntu:22.04"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                Docker.Literal imageName = (Docker.Literal) from.getImageName().getContents().getFirst();
                assertThat(imageName.getText()).isEqualTo("ubuntu:22.04");
                assertThat(imageName.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(from.getTag()).isNull();
            })
          )
        );
    }

    @Test
    void quotedTagKeepsItsQuoteStyle() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:"22.04"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                Docker.Literal tag = (Docker.Literal) from.getTag().getContents().getFirst();
                assertThat(tag.getText()).isEqualTo("22.04");
                assertThat(tag.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
            })
          )
        );
    }

    @Test
    void separatorWithoutATag() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getImageName().getContents().getFirst()).getText()).isEqualTo("ubuntu");
                assertThat(from.getTag().getContents()).isEmpty();
            })
          )
        );
    }

    @Test
    void continuationBeforeTagSeparator() {
        rewriteRun(
          docker(
            """
              FROM ubuntu\\
                :22.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
            })
          )
        );
    }

    /// Spaces and tabs may sit between the escape character and the newline it continues over, so the
    /// name ends before the escape character rather than carrying it and losing the tag with it.
    @Test
    void continuationPaddedWithSpacesBeforeTagSeparator() {
        rewriteRun(
          docker(
            """
              FROM ubuntu\\  \s
                :22.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
            })
          )
        );
    }

    /// A backtick continues a line as a backslash does, since the lexer reads both without asking which
    /// one the `escape` directive names.
    @Test
    void backtickContinuationBeforeTagSeparator() {
        rewriteRun(
          docker(
            """
              FROM ubuntu`
                :22.04
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getFirst().getFrom();
                assertThat(((Docker.Literal) from.getTag().getContents().getFirst()).getText()).isEqualTo("22.04");
            })
          )
        );
    }

    @Test
    void quotedImageReferenceWithVariableStaysWhole() {
        rewriteRun(
          docker(
            """
              ARG TAG=22.04
              FROM "ubuntu:${TAG}"
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.From from = doc.getStages().getLast().getFrom();
                assertThat(ArgumentContents.textWithVariables(from.getImageName())).isEqualTo("\"ubuntu:${TAG}\"");
                assertThat(from.getTag()).isNull();
                assertThat(ArgumentContents.containsVariable(from.getImageName())).isTrue();
            })
          )
        );
    }

    @Test
    void lowercaseArgReferenceInTag() {
        rewriteRun(
          docker(
            """
              ARG java_version=17
              FROM eclipse-temurin:${java_version}
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument tag = doc.getStages().getLast().getFrom().getTag();
                assertThat(tag).isNotNull();
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) tag.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("java_version");
                assertThat(var.isBraced()).isTrue();
                assertThat(ArgumentContents.containsVariable(tag)).isTrue();
                assertThat(ArgumentContents.text(tag)).isNull();
                assertThat(ArgumentContents.textWithVariables(tag)).isEqualTo("${java_version}");
            })
          )
        );
    }

    @Test
    void unbracedLowercaseArgReferenceInTag() {
        rewriteRun(
          docker(
            """
              ARG java_version=17
              FROM eclipse-temurin:$java_version
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument tag = doc.getStages().getLast().getFrom().getTag();
                assertThat(tag).isNotNull();
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) tag.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("java_version");
                assertThat(var.isBraced()).isFalse();
                assertThat(ArgumentContents.text(tag)).isNull();
                assertThat(ArgumentContents.textWithVariables(tag)).isEqualTo("$java_version");
            })
          )
        );
    }

    @Test
    void mixedCaseArgReferenceInTag() {
        rewriteRun(
          docker(
            """
              ARG Java_Version=17
              FROM eclipse-temurin:${Java_Version}
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument tag = doc.getStages().getLast().getFrom().getTag();
                assertThat(tag).isNotNull();
                assertThat(((Docker.EnvironmentVariable) tag.getContents().getFirst()).getName()).isEqualTo("Java_Version");
                assertThat(ArgumentContents.text(tag)).isNull();
                assertThat(ArgumentContents.textWithVariables(tag)).isEqualTo("${Java_Version}");
            })
          )
        );
    }

    @Test
    void lowercaseVariableInPlatformFlag() {
        rewriteRun(
          docker(
            """
              FROM --platform=$target_platform alpine:latest
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument flagValue = doc.getStages().getFirst().getFrom().getFlags().getFirst().getValue();
                assertThat(flagValue).isNotNull();
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) flagValue.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("target_platform");
                assertThat(ArgumentContents.text(flagValue)).isNull();
                assertThat(ArgumentContents.textWithVariables(flagValue)).isEqualTo("$target_platform");
            })
          )
        );
    }

    @Test
    void platformFlagKeepsVariableDefault() {
        rewriteRun(
          docker(
            """
              FROM --platform=${TARGETPLATFORM:-linux/amd64} alpine:latest
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Argument flagValue = doc.getStages().getFirst().getFrom().getFlags().getFirst().getValue();
                assertThat(flagValue).isNotNull();
                Docker.EnvironmentVariable var = (Docker.EnvironmentVariable) flagValue.getContents().getFirst();
                assertThat(var.getName()).isEqualTo("TARGETPLATFORM:-linux/amd64");
                assertThat(var.isBraced()).isTrue();
                assertThat(ArgumentContents.textWithVariables(flagValue)).isEqualTo("${TARGETPLATFORM:-linux/amd64}");
            })
          )
        );
    }
}
