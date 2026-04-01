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

class AddAptGetCleanupTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddAptGetCleanup(null));
    }

    @DocumentExample
    @Test
    void addCleanupToSimpleInstall() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void addCleanupToInstallWithoutFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void skipWhenCleanupAlreadyPresent() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void skipNonAptGetCommands() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void skipApkCommands() {
        rewriteRun(
          docker(
            """
              FROM alpine:3.18
              RUN apk add --no-cache curl
              """
          )
        );
    }

    @Test
    void customCleanupCommand() {
        rewriteRun(
          spec -> spec.recipe(new AddAptGetCleanup(" && apt-get clean && rm -rf /var/lib/apt/lists/*")),
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && apt-get clean && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void multipleAptGetInstallsInDifferentRuns() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              RUN apt-get install -y vim
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              RUN apt-get install -y vim && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void multiStage() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04 AS builder
              RUN apt-get update && apt-get install -y build-essential

              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl
              """,
            """
              FROM ubuntu:22.04 AS builder
              RUN apt-get update && apt-get install -y build-essential && rm -rf /var/lib/apt/lists/*

              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void aptGetWithMultipleFlags() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get -y -q install curl
              """,
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get -y -q install curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }

    @Test
    void skipVariantCleanupWithDifferentPath() {
        // The cleanup pattern checks for /var/lib/apt/lists specifically
        rewriteRun(
          docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
              """
          )
        );
    }
}
