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
package org.openrewrite.maven.parity.synthetic;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * A jar published without a pom: the legacy engine HEAD-probes the sibling jar after the pom
 * 404s and synthesizes a stub jar-packaging pom for it. Maven's missing-descriptor tolerance
 * ("The POM for ... is missing, no dependency information available", then proceed) is the
 * parity reference for Phase 3; effective behavior matches (a4 catalog #64).
 */
class PomlessJarTest {
    private static final String G = "org.parity.synthetic";
    private static final String POM = pomPath(G, "nopom", "1.0", "1.0");
    private static final String JAR = POM.replace(".pom", ".jar");

    @Test
    void jarWithoutPomResolvesThroughSynthesizedPom() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            // The jar exists (200 on HEAD, body stripped by MockMavenRepo); the pom does not
            repo.serve(JAR, request -> new MockResponse().setResponseCode(200));

            SyntheticHarness.Session session = new SyntheticHarness.Session(
              ctx -> ctx.setRepositories(List.of(repo.repo("mock"))));
            SyntheticHarness.Resolution resolution = session.resolve(rootPom(dependencies(G + ":nopom:1.0")));

            assertThat(resolution.failed()).isFalse();
            assertThat(repo.requests()).containsExactly("GET " + POM, "HEAD " + JAR);

            var node = resolution.snapshot().getJson().at("/scopes/Compile/0");
            assertThat(node.get("gav").asText()).isEqualTo(G + ":nopom:1.0");
            assertThat(node.get("type").asText()).isEqualTo("jar");
            assertThat(node.get("children")).isEmpty();

            // Both the pom miss and the eventual success are visible in the event stream
            var events = resolution.snapshot().getJson().get("events");
            assertThat(events.fieldNames()).toIterable()
              .anyMatch(e -> e.startsWith("downloadError:" + G + ":nopom:1.0"))
              .anyMatch(e -> e.startsWith("downloadSuccess:" + G + ":nopom:1.0"));

            // No errors surfaced: the synthesized pom makes the dependency resolvable
            assertThat(resolution.errors()).isEmpty();
        }
    }
}
