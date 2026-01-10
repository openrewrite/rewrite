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
import org.openrewrite.docker.table.DockerBaseImages;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class FindDockerBaseImagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindDockerBaseImages(null));
    }

    @DocumentExample
    @Test
    void findAllBaseImages() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,ubuntu,22.04,,,
              """
          ),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              ~~(ubuntu:22.04)~~>FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void findBaseImageWithPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindDockerBaseImages("ubuntu*"))
            .dataTableAsCsv(DockerBaseImages.class.getName(),
              //language=csv
              """
                sourceFile,imageName,tag,digest,platform,stageName
                Dockerfile,ubuntu,22.04,,,
                """
            ),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """,
            """
              ~~(ubuntu:22.04)~~>FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void noMatchWithPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindDockerBaseImages("alpine*")),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void findBaseImageWithDigest() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,ubuntu,,sha256:abc123,,
              """
          ),
          Assertions.docker(
            """
              FROM ubuntu@sha256:abc123
              RUN apt-get update
              """,
            """
              ~~(ubuntu@sha256:abc123)~~>FROM ubuntu@sha256:abc123
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void findBaseImageWithPlatform() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,ubuntu,22.04,,linux/amd64,
              """
          ),
          Assertions.docker(
            """
              FROM --platform=linux/amd64 ubuntu:22.04
              RUN apt-get update
              """,
            """
              ~~(ubuntu:22.04)~~>FROM --platform=linux/amd64 ubuntu:22.04
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void findBaseImageWithStageName() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,golang,1.21,,,builder
              """
          ),
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .
              """,
            """
              ~~(golang:1.21)~~>FROM golang:1.21 AS builder
              RUN go build -o app .
              """
          )
        );
    }

    @Test
    void findMultipleBaseImages() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,golang,1.21,,,builder
              Dockerfile,alpine,latest,,,
              """
          ),
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:latest
              COPY --from=builder /app /app
              """,
            """
              ~~(golang:1.21)~~>FROM golang:1.21 AS builder
              RUN go build -o app .

              ~~(alpine:latest)~~>FROM alpine:latest
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void filterByPatternInMultiStage() {
        rewriteRun(
          spec -> spec.recipe(new FindDockerBaseImages("alpine*"))
            .dataTableAsCsv(DockerBaseImages.class.getName(),
              //language=csv
              """
                sourceFile,imageName,tag,digest,platform,stageName
                Dockerfile,alpine,latest,,,
                """
            ),
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:latest
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              ~~(alpine:latest)~~>FROM alpine:latest
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void findBaseImageWithAllDetails() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,ubuntu,22.04,,linux/arm64,base
              """
          ),
          Assertions.docker(
            """
              FROM --platform=linux/arm64 ubuntu:22.04 AS base
              RUN apt-get update
              """,
            """
              ~~(ubuntu:22.04)~~>FROM --platform=linux/arm64 ubuntu:22.04 AS base
              RUN apt-get update
              """
          )
        );
    }

    @Test
    void findBaseImageWithoutTag() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerBaseImages.class.getName(),
            //language=csv
            """
              sourceFile,imageName,tag,digest,platform,stageName
              Dockerfile,ubuntu,,,,
              """
          ),
          Assertions.docker(
            """
              FROM ubuntu
              RUN apt-get update
              """,
            """
              ~~(ubuntu)~~>FROM ubuntu
              RUN apt-get update
              """
          )
        );
    }
}
