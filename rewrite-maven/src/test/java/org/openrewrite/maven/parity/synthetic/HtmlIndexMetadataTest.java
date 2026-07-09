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
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Metadata derivation from a Nexus/Artifactory-style HTML index when {@code maven-metadata.xml}
 * 404s (a2 §1.3, a4 catalog #66). This AUGMENTS Maven — real Maven has no equivalent and its
 * keep/kill call is still open; these tests pin what exists either way. Ledger L-P0-006.
 */
class HtmlIndexMetadataTest {
    private static final String G = "org.parity.synthetic";

    private static String rangeDependency(String artifactId) {
        //language=xml
        return """
          <dependency>
              <groupId>org.parity.synthetic</groupId>
              <artifactId>%s</artifactId>
              <version>[1.0,2.0]</version>
          </dependency>
          """.formatted(artifactId);
    }

    @Test
    void derivesVersionsFromHtmlIndexWhenMetadataMissing() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            // maven-metadata.xml 404s (default); the artifact directory serves an HTML index
            repo.serve("/org/parity/synthetic/idx/", """
              <html><body>
              <a href="../">../</a>
              <a href="1.0/">1.0/</a>
              <a href="2.0/">2.0/</a>
              </body></html>
              """);
            repo.serve(pomPath(G, "idx", "2.0", "2.0"), pomXml(G, "idx", "2.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom("<dependencies>" + rangeDependency("idx") + "</dependencies>"),
              ctx -> ctx.setRepositories(List.of(repo.repo("nexus"))));

            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/gav").asText())
              .isEqualTo(G + ":idx:2.0");
            // Metadata miss, index scrape, then the pom of the highest in-range derived version
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly(
                  "GET /org/parity/synthetic/idx/maven-metadata.xml",
                  "GET /org/parity/synthetic/idx/",
                  "GET " + pomPath(G, "idx", "2.0", "2.0"));
            }
        }
    }

    /**
     * A non-404 client error on the index scrape disables derivation on that repository for the
     * rest of the run: the second artifact's index is never requested.
     */
    @Test
    void accessDeniedDisablesDerivationForTheRestOfTheRun() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve("/org/parity/synthetic/idx1/", request -> new MockResponse().setResponseCode(403));
            repo.serve("/org/parity/synthetic/idx2/", """
              <html><body><a href="1.0/">1.0/</a></body></html>
              """);

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom("<dependencies>" + rangeDependency("idx1") + rangeDependency("idx2") + "</dependencies>"),
              ctx -> ctx.setRepositories(List.of(repo.repo("nexus"))));

            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.errored()).isTrue();
            assertThat(resolution.errors()).isNotEmpty();
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly(
                  "GET /org/parity/synthetic/idx1/maven-metadata.xml",
                  "GET /org/parity/synthetic/idx1/",
                  "GET /org/parity/synthetic/idx2/maven-metadata.xml");
            }
        }
    }
}
