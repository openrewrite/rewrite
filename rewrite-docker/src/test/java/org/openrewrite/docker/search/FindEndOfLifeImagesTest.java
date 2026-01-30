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
package org.openrewrite.docker.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.docker.Assertions;
import org.openrewrite.docker.table.EolDockerImages;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class FindEndOfLifeImagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindEndOfLifeImages(null));
    }

    @DocumentExample
    @Test
    void detectDebianBuster() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(EolDockerImages.class.getName(),
            //language=csv
            """
              sourceFile,stageName,imageName,tag,eolDate,suggestedReplacement
              Dockerfile,,debian,buster,2024-06-30,"trixie (13) or bookworm (12)"
              """
          ),
          Assertions.docker(
            """
              FROM debian:buster
              RUN apt-get update
              """,
            """
              ~~(EOL: debian:buster (ended 2024-06-30, suggest trixie (13) or bookworm (12)))~~>FROM debian:buster
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectDebianBusterSlim() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(EolDockerImages.class.getName(),
            //language=csv
            """
              sourceFile,stageName,imageName,tag,eolDate,suggestedReplacement
              Dockerfile,,debian,buster-slim,2024-06-30,"trixie (13) or bookworm (12)"
              """
          ),
          Assertions.docker(
            """
              FROM debian:buster-slim
              RUN apt-get update
              """,
            """
              ~~(EOL: debian:buster-slim (ended 2024-06-30, suggest trixie (13) or bookworm (12)))~~>FROM debian:buster-slim
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectDebianStretch() {
        rewriteRun(
          Assertions.docker(
            """
              FROM debian:stretch
              RUN apt-get update
              """,
            """
              ~~(EOL: debian:stretch (ended 2022-07-01, suggest trixie (13) or bookworm (12)))~~>FROM debian:stretch
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectUbuntuXenial() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:16.04
              RUN apt-get update
              """,
            """
              ~~(EOL: ubuntu:16.04 (ended 2021-04-30, suggest noble (24.04)))~~>FROM ubuntu:16.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectUbuntuBionic() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:bionic
              RUN apt-get update
              """,
            """
              ~~(EOL: ubuntu:bionic (ended 2023-05-31, suggest noble (24.04)))~~>FROM ubuntu:bionic
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void detectAlpine3_14() {
        rewriteRun(
          Assertions.docker(
            """
              FROM alpine:3.14
              RUN apk update
              """,
            """
              ~~(EOL: alpine:3.14 (ended 2023-05-01, suggest 3.23 or 3.22))~~>FROM alpine:3.14
              RUN apk update
              """
          )
        );
    }

    @Test
    void detectPython37() {
        rewriteRun(
          Assertions.docker(
            """
              FROM python:3.7
              RUN pip install flask
              """,
            """
              ~~(EOL: python:3.7 (ended 2023-06-27, suggest 3.14 or 3.13))~~>FROM python:3.7
              RUN pip install flask
              """
          )
        );
    }

    @Test
    void detectPython37Alpine() {
        rewriteRun(
          Assertions.docker(
            """
              FROM python:3.7-alpine
              RUN pip install flask
              """,
            """
              ~~(EOL: python:3.7-alpine (ended 2023-06-27, suggest 3.14 or 3.13))~~>FROM python:3.7-alpine
              RUN pip install flask
              """
          )
        );
    }

    @Test
    void detectNode14() {
        rewriteRun(
          Assertions.docker(
            """
              FROM node:14-slim
              RUN npm install
              """,
            """
              ~~(EOL: node:14-slim (ended 2023-04-30, suggest 22 or 20))~~>FROM node:14-slim
              RUN npm install
              """
          )
        );
    }

    @Test
    void detectNode16() {
        rewriteRun(
          Assertions.docker(
            """
              FROM node:16
              RUN npm install
              """,
            """
              ~~(EOL: node:16 (ended 2024-04-30, suggest 24 or 22))~~>FROM node:16
              RUN npm install
              """
          )
        );
    }

    @Test
    void currentDebianNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM debian:bookworm
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void currentUbuntuNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM ubuntu:24.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void currentAlpineNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM alpine:3.21
              RUN apk update
              """
          )
        );
    }

    @Test
    void currentPythonNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM python:3.12
              RUN pip install flask
              """
          )
        );
    }

    @Test
    void currentNodeNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM node:22
              RUN npm install
              """
          )
        );
    }

    @Test
    void scratchImageNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM scratch
              COPY app /app
              """
          )
        );
    }

    @Test
    void imageWithoutTagNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM debian
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void multiStageDetectsAllEol() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(EolDockerImages.class.getName(),
            //language=csv
            """
              sourceFile,stageName,imageName,tag,eolDate,suggestedReplacement
              Dockerfile,builder,node,14,2023-04-30,"22 or 20"
              Dockerfile,,debian,buster,2024-06-30,"trixie (13) or bookworm (12)"
              """
          ),
          Assertions.docker(
            """
              FROM node:14 AS builder
              RUN npm run build

              FROM debian:buster
              COPY --from=builder /app /app
              """,
            """
              ~~(EOL: node:14 (ended 2023-04-30, suggest 22 or 20))~~>FROM node:14 AS builder
              RUN npm run build

              ~~(EOL: debian:buster (ended 2024-06-30, suggest trixie (13) or bookworm (12)))~~>FROM debian:buster
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void mixedEolAndCurrent() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(EolDockerImages.class.getName(),
            //language=csv
            """
              sourceFile,stageName,imageName,tag,eolDate,suggestedReplacement
              Dockerfile,,debian,buster,2024-06-30,"trixie (13) or bookworm (12)"
              """
          ),
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM debian:buster
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              ~~(EOL: debian:buster (ended 2024-06-30, suggest trixie (13) or bookworm (12)))~~>FROM debian:buster
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void unknownImageNotFlagged() {
        rewriteRun(
          Assertions.docker(
            """
              FROM mycompany/custom-image:1.0
              RUN echo hello
              """
          )
        );
    }

    @Test
    void detectDebianByVersionNumber() {
        rewriteRun(
          Assertions.docker(
            """
              FROM debian:10
              RUN apt-get update
              """,
            """
              ~~(EOL: debian:10 (ended 2024-06-30, suggest trixie (13) or bookworm (12)))~~>FROM debian:10
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void envVarTagNotFlagged() {
        // Cannot determine if EOL when tag contains environment variable
        rewriteRun(
          Assertions.docker(
            """
              ARG DEBIAN_VERSION=buster
              FROM debian:${DEBIAN_VERSION}
              RUN apt-get update
              """
          )
        );
    }
}
