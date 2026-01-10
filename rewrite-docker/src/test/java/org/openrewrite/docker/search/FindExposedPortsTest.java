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
import org.openrewrite.docker.table.DockerExposedPorts;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class FindExposedPortsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindExposedPorts(null));
    }

    @DocumentExample
    @Test
    void findSinglePort() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,80,,,false
              """
          ),
          Assertions.docker(
            """
              FROM nginx:latest
              EXPOSE 80
              """,
            """
              FROM nginx:latest
              ~~(80)~~>EXPOSE 80
              """
          )
        );
    }

    @Test
    void findMultiplePorts() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,80,,,false
              Dockerfile,443,,,false
              """
          ),
          Assertions.docker(
            """
              FROM nginx:latest
              EXPOSE 80 443
              """,
            """
              FROM nginx:latest
              ~~(80, 443)~~>EXPOSE 80 443
              """
          )
        );
    }

    @Test
    void findPortWithProtocol() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,53,udp,,false
              Dockerfile,53,tcp,,false
              """
          ),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              EXPOSE 53/udp 53/tcp
              """,
            """
              FROM ubuntu:22.04
              ~~(53/udp, 53/tcp)~~>EXPOSE 53/udp 53/tcp
              """
          )
        );
    }

    @Test
    void findPortRange() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,8000-8100,,,true
              """
          ),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              EXPOSE 8000-8100
              """,
            """
              FROM ubuntu:22.04
              ~~(8000-8100)~~>EXPOSE 8000-8100
              """
          )
        );
    }

    @Test
    void findPortWithStageName() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,8080,,app,false
              """
          ),
          Assertions.docker(
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18 AS app
              EXPOSE 8080
              COPY --from=builder /app /app
              """,
            """
              FROM golang:1.21 AS builder
              RUN go build -o app .

              FROM alpine:3.18 AS app
              ~~(8080)~~>EXPOSE 8080
              COPY --from=builder /app /app
              """
          )
        );
    }

    @Test
    void filterByPortPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindExposedPorts("80*"))
            .dataTableAsCsv(DockerExposedPorts.class.getName(),
              """
                sourceFile,port,protocol,stageName,range
                Dockerfile,80,,,false
                Dockerfile,8080,,,false
                """
            ),
          Assertions.docker(
            """
              FROM nginx:latest
              EXPOSE 80 443 8080
              """,
            """
              FROM nginx:latest
              ~~(80, 8080)~~>EXPOSE 80 443 8080
              """
          )
        );
    }

    @Test
    void filterByProtocol() {
        rewriteRun(
          spec -> spec.recipe(new FindExposedPorts("*/udp"))
            .dataTableAsCsv(DockerExposedPorts.class.getName(),
              """
                sourceFile,port,protocol,stageName,range
                Dockerfile,53,udp,,false
                """
            ),
          Assertions.docker(
            """
              FROM ubuntu:22.04
              EXPOSE 53/udp 53/tcp
              """,
            """
              FROM ubuntu:22.04
              ~~(53/udp)~~>EXPOSE 53/udp 53/tcp
              """
          )
        );
    }

    @Test
    void noMatchWithPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindExposedPorts("9999")),
          Assertions.docker(
            """
              FROM nginx:latest
              EXPOSE 80 443
              """
          )
        );
    }

    @Test
    void multipleExposeInstructions() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(DockerExposedPorts.class.getName(),
            """
              sourceFile,port,protocol,stageName,range
              Dockerfile,80,,,false
              Dockerfile,443,,,false
              """
          ),
          Assertions.docker(
            """
              FROM nginx:latest
              EXPOSE 80
              EXPOSE 443
              """,
            """
              FROM nginx:latest
              ~~(80)~~>EXPOSE 80
              ~~(443)~~>EXPOSE 443
              """
          )
        );
    }

}
