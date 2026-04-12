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

class ReplaceAddWithCopyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceAddWithCopy());
    }

    @DocumentExample
    @Test
    void replaceSimpleAdd() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD app.jar /app/
              """,
            """
              FROM ubuntu:22.04
              COPY app.jar /app/
              """
          )
        );
    }

    @Test
    void replaceAddWithMultipleSources() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD file1.txt file2.txt /app/
              """,
            """
              FROM ubuntu:22.04
              COPY file1.txt file2.txt /app/
              """
          )
        );
    }

    @Test
    void preserveAddForUrl() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD https://example.com/file.tar.gz /tmp/
              """
          )
        );
    }

    @Test
    void preserveAddForHttpsUrl() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD https://github.com/releases/app.tar.gz /tmp/
              """
          )
        );
    }

    @Test
    void preserveAddForTarGz() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.tar.gz /app/
              """
          )
        );
    }

    @Test
    void preserveAddForTar() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.tar /app/
              """
          )
        );
    }

    @Test
    void preserveAddForTgz() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.tgz /app/
              """
          )
        );
    }

    @Test
    void preserveAddForTarBz2() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.tar.bz2 /app/
              """
          )
        );
    }

    @Test
    void preserveAddForTarXz() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.tar.xz /app/
              """
          )
        );
    }

    @Test
    void replaceAddForZip() {
        // ZIP files are NOT auto-extracted by ADD, so we can use COPY
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD archive.zip /app/
              """,
            """
              FROM ubuntu:22.04
              COPY archive.zip /app/
              """
          )
        );
    }

    @Test
    void replaceAddWithFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD --chown=user:group app.jar /app/
              """,
            """
              FROM ubuntu:22.04
              COPY --chown=user:group app.jar /app/
              """
          )
        );
    }

    @Test
    void preserveAddWithEnvVarSource() {
        // Can't determine if it's a URL or tar at compile time
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD $SOURCE_FILE /app/
              """
          )
        );
    }

    @Test
    void multipleStages() {
        rewriteRun(
          docker(
            """
              FROM golang:1.21 AS builder
              ADD main.go /src/

              FROM alpine:3.18
              ADD app /usr/local/bin/
              """,
            """
              FROM golang:1.21 AS builder
              COPY main.go /src/

              FROM alpine:3.18
              COPY app /usr/local/bin/
              """
          )
        );
    }

    @Test
    void mixedAddInstructions() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              ADD app.jar /app/
              ADD https://example.com/config.tar.gz /tmp/
              ADD config.yml /app/
              """,
            """
              FROM ubuntu:22.04
              COPY app.jar /app/
              ADD https://example.com/config.tar.gz /tmp/
              COPY config.yml /app/
              """
          )
        );
    }
}
