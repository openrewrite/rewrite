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
package org.openrewrite.java.internal.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.internal.parser.RewriteClasspathJarClasspathLoader.selectJar;

class RewriteClasspathJarClasspathLoaderTest {

    private static final String TRANSPORT = "META-INF/rewrite/classpath/netty-transport-4.2.19.Final.jar";
    private static final String EPOLL = "META-INF/rewrite/classpath/netty-transport-classes-epoll-4.2.19.Final.jar";

    @ParameterizedTest
    @CsvSource({
      "netty-transport, " + TRANSPORT,
      "netty-transport-4, " + TRANSPORT,
      "netty-transport-4.2.+, " + TRANSPORT,
      "netty-transport-classes-epoll, " + EPOLL,
      "netty-transport-classes-epoll-4, " + EPOLL
    })
    void prefersJarOfTheNamedArtifact(String artifactName, String expected) {
        assertThat(selectJar(artifactName, List.of(EPOLL, TRANSPORT))).isEqualTo(expected);
        assertThat(selectJar(artifactName, List.of(TRANSPORT, EPOLL))).isEqualTo(expected);
    }

    @Test
    void fallsBackToPrefixOfArtifactId() {
        assertThat(selectJar("netty-transport", List.of(EPOLL))).isEqualTo(EPOLL);
    }

    @Test
    void fallsBackToShortestArtifactIdTheNameIsAPrefixOf() {
        assertThat(selectJar("netty-trans", List.of(EPOLL, TRANSPORT))).isEqualTo(TRANSPORT);
        assertThat(selectJar("netty-trans", List.of(TRANSPORT, EPOLL))).isEqualTo(TRANSPORT);
    }

    @Test
    void artifactIdsContainingDigits() {
        String log4j12Api = "META-INF/rewrite/classpath/log4j-1.2-api-2.20.0.jar";
        String log4j = "META-INF/rewrite/classpath/log4j-1.2.17.jar";

        assertThat(selectJar("log4j-1.2-api", List.of(log4j, log4j12Api))).isEqualTo(log4j12Api);
        assertThat(selectJar("log4j-1.2.17", List.of(log4j12Api, log4j))).isEqualTo(log4j);
    }

    @Test
    void jarWithoutVersion() {
        String guava = "META-INF/rewrite/classpath/guava.jar";

        assertThat(selectJar("guava", List.of(guava))).isEqualTo(guava);
    }
}
