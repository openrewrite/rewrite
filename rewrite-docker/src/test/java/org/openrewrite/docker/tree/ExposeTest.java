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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class ExposeTest implements RewriteTest {

    @Test
    void exposeInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8080
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(1);
                Docker.Port port = expose.getPorts().getFirst();
                assertThat(port.getText()).isEqualTo("8080");
                assertThat(port.getStart()).isEqualTo(8080);
                assertThat(port.getEnd()).isNull();
                assertThat(port.getProtocol()).isEqualTo(Docker.Port.Protocol.TCP);
            })
          )
        );
    }

    @Test
    void exposeMultiplePorts() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8080 8443
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(2);
                assertThat(expose.getPorts().get(0).getText()).isEqualTo("8080");
                assertThat(expose.getPorts().get(0).getStart()).isEqualTo(8080);
                assertThat(expose.getPorts().get(1).getText()).isEqualTo("8443");
                assertThat(expose.getPorts().get(1).getStart()).isEqualTo(8443);
            })
          )
        );
    }

    @Test
    void exposeWithEnvironmentVariable() {
        // EXPOSE with environment variable reference
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              ENV PORT=8080
              EXPOSE ${PORT}
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Port port = expose.getPorts().getFirst();
                assertThat(port.getText()).isEqualTo("${PORT}");
                assertThat(port.isVariable()).isTrue();
                assertThat(port.getStart()).isNull();
                assertThat(port.getEnd()).isNull();
            })
          )
        );
    }

    @Test
    void exposeWithProtocol() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8080/tcp 53/udp
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(expose.getPorts()).hasSize(2);

                Docker.Port tcpPort = expose.getPorts().get(0);
                assertThat(tcpPort.getText()).isEqualTo("8080/tcp");
                assertThat(tcpPort.getStart()).isEqualTo(8080);
                assertThat(tcpPort.getProtocol()).isEqualTo(Docker.Port.Protocol.TCP);

                Docker.Port udpPort = expose.getPorts().get(1);
                assertThat(udpPort.getText()).isEqualTo("53/udp");
                assertThat(udpPort.getStart()).isEqualTo(53);
                assertThat(udpPort.getProtocol()).isEqualTo(Docker.Port.Protocol.UDP);
            })
          )
        );
    }

    @Test
    void exposePortRange() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8000-9000
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Port port = expose.getPorts().getFirst();
                assertThat(port.getText()).isEqualTo("8000-9000");
                assertThat(port.getStart()).isEqualTo(8000);
                assertThat(port.getEnd()).isEqualTo(9000);
                assertThat(port.isRange()).isTrue();
                assertThat(port.getProtocol()).isEqualTo(Docker.Port.Protocol.TCP);
            })
          )
        );
    }

    @Test
    void exposePortRangeWithProtocol() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              EXPOSE 8000-9000/udp
              """,
            spec -> spec.afterRecipe(doc -> {
                Docker.Expose expose = (Docker.Expose) doc.getStages().getFirst().getInstructions().getLast();
                Docker.Port port = expose.getPorts().getFirst();
                assertThat(port.getText()).isEqualTo("8000-9000/udp");
                assertThat(port.getStart()).isEqualTo(8000);
                assertThat(port.getEnd()).isEqualTo(9000);
                assertThat(port.isRange()).isTrue();
                assertThat(port.getProtocol()).isEqualTo(Docker.Port.Protocol.UDP);
            })
          )
        );
    }
}
