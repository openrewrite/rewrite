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
package org.openrewrite.gradle.marker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.test.MockHttpSender;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A dynamic version declared in a build file names a set of candidate versions rather than an
 * artifact, so it has to be resolved against the module's {@code maven-metadata.xml} before a POM can
 * be requested. A POM request built from the selector string asks for a path that cannot exist, and
 * the resulting failure silently leaves the configuration's previously resolved dependencies in place.
 */
class GradleDependencyConfigurationResolutionTest {

    private static final String GROUP_ID = "com.example.dynamic";
    private static final String ARTIFACT_ID = "example-lib";
    private static final String MODULE_URI = MavenRepository.MAVEN_CENTRAL.getUri() + '/' + GROUP_ID.replace('.', '/') + '/' + ARTIFACT_ID;

    private static final String METADATA = """
      <metadata>
          <groupId>com.example.dynamic</groupId>
          <artifactId>example-lib</artifactId>
          <versioning>
              <latest>2.0.0</latest>
              <release>2.0.0</release>
              <versions>
                  <version>1.2.3</version>
                  <version>1.3.0</version>
                  <version>2.0.0</version>
              </versions>
              <lastUpdated>20240101000000</lastUpdated>
          </versioning>
      </metadata>
      """;

    private final List<String> requestedUrls = new ArrayList<>();

    @CsvSource({
      "latest.release,2.0.0",
      "latest.integration,2.0.0",
      "latest.snapshot,2.0.0",
      "LATEST,2.0.0",
      "RELEASE,2.0.0",
      "1.+,1.3.0",
      "1.x,1.3.0",
      "'[1.0,2.0)',1.3.0"
    })
    @ParameterizedTest
    void dynamicVersionIsResolvedBeforeAnyPomIsRequested(String declaredVersion, String expectedVersion) {
        List<ResolvedDependency> resolved = resolveDirectDependency(declaredVersion);

        assertThat(requestedUrls)
          .as("a POM must never be requested at a path built from the version selector")
          .noneMatch(url -> url.contains(declaredVersion));
        assertThat(requestedUrls).anyMatch(url -> url.endsWith("maven-metadata.xml"));
        assertThat(resolved).singleElement()
          .satisfies(d -> assertThat(d.getGav().getVersion()).isEqualTo(expectedVersion));
    }

    @Test
    void concreteVersionIsRequestedDirectly() {
        List<ResolvedDependency> resolved = resolveDirectDependency("1.2.3");

        assertThat(requestedUrls)
          .as("a concrete version needs no metadata lookup")
          .containsExactly(MODULE_URI + "/1.2.3/example-lib-1.2.3.pom");
        assertThat(resolved).singleElement()
          .satisfies(d -> assertThat(d.getGav().getVersion()).isEqualTo("1.2.3"));
    }

    @Test
    void unsatisfiableDynamicVersionRequestsNoPom() {
        List<ResolvedDependency> resolved = resolveDirectDependency("9.+");

        assertThat(requestedUrls).containsExactly(MODULE_URI + "/maven-metadata.xml");
        assertThat(resolved).isEmpty();
    }

    @Test
    void unmodelledLatestStatusRequestsNothing() {
        List<ResolvedDependency> resolved = resolveDirectDependency("latest.milestone");

        assertThat(requestedUrls).isEmpty();
        assertThat(resolved).isEmpty();
    }

    private List<ResolvedDependency> resolveDirectDependency(String declaredVersion) {
        Dependency requested = Dependency.builder()
          .gav(new GroupArtifactVersion(GROUP_ID, ARTIFACT_ID, declaredVersion))
          .scope("implementation")
          .build();
        GradleDependencyConfiguration configuration = GradleDependencyConfiguration.builder()
          .name("implementation")
          .isTransitive(true)
          .isCanBeResolved(true)
          .extendsFrom(emptyList())
          .requested(singletonList(requested))
          .directResolved(emptyList())
          .build();

        ExecutionContext ctx = new InMemoryExecutionContext();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(this::respond);
        configuration.markForReResolution(singletonList(MavenRepository.MAVEN_CENTRAL), ctx);

        return configuration.getDirectResolved();
    }

    private HttpSender.Response respond(HttpSender.Request request) {
        String url = request.getUrl().toString();
        requestedUrls.add(url);
        if (url.endsWith("maven-metadata.xml")) {
            return body(request, METADATA);
        }
        String pomName = url.substring(url.lastIndexOf('/') + 1);
        if (pomName.startsWith(ARTIFACT_ID + '-') && pomName.endsWith(".pom")) {
            String version = pomName.substring(ARTIFACT_ID.length() + 1, pomName.length() - ".pom".length());
            return body(request, """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
              </project>
              """.formatted(GROUP_ID, ARTIFACT_ID, version));
        }
        return new MockHttpSender(404).send(request);
    }

    private static HttpSender.Response body(HttpSender.Request request, String body) {
        return new MockHttpSender(() -> new ByteArrayInputStream(body.getBytes(UTF_8))).send(request);
    }
}
