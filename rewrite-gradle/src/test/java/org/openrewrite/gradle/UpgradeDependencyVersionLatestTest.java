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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.MockHttpSender;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.openrewrite.gradle.Assertions.buildGradle;

class UpgradeDependencyVersionLatestTest implements RewriteTest {
    private static final MavenRepository REPOSITORY = MavenRepository.builder()
      .id("test")
      .uri("https://repo.example.test/maven2")
      .knownToExist(true)
      .build();

    @ParameterizedTest
    @CsvSource({
      "RELEASE, , 2.0.0",
      "release, , 2.0.0",
      "latest.release, , 2.0.0",
      "LATEST, , 3.0.0-RC1",
      "Latest, , 3.0.0-RC1",
      "latest.integration, , 3.0.0-RC1",
      "RELEASE, -jre, 2.0.0-jre",
      "latest.release, -jre, 2.0.0-jre",
      "LATEST, -jre, 2.0.0-jre",
      "latest.integration, -jre, 2.0.0-jre",
      "2.0.0, , 2.0.0"
    })
    void latestSelectors(String selector, @Nullable String versionPattern, String expected) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.example", "my-lib", selector, versionPattern))
            .executionContext(repositoryContext())
            .cycles(2).expectedCyclesThatMakeChanges(1),
          buildGradle(
            """
              dependencies {
                  implementation 'com.example:my-lib:1.0.0'
              }
              """,
            """
              dependencies {
                  implementation 'com.example:my-lib:%s'
              }
              """.formatted(expected),
            spec -> spec.markers(project())
          )
        );
    }

    private static GradleProject project() {
        Dependency requested = Dependency.builder()
          .gav(new GroupArtifactVersion("com.example", "my-lib", "1.0.0"))
          .scope("implementation")
          .build();
        ResolvedDependency resolved = ResolvedDependency.builder()
          .repository(REPOSITORY)
          .gav(new ResolvedGroupArtifactVersion(null, "com.example", "my-lib", "1.0.0", null))
          .requested(requested)
          .depth(0)
          .build();
        GradleDependencyConfiguration implementation = GradleDependencyConfiguration.builder()
          .name("implementation")
          .isCanBeResolved(true)
          .isTransitive(true)
          .extendsFrom(List.of())
          .requested(List.of(requested))
          .directResolved(List.of(resolved))
          .build();
        return GradleProject.builder()
          .mavenRepositories(List.of(REPOSITORY))
          .nameToConfiguration(singletonMap("implementation", implementation))
          .build();
    }

    private static ExecutionContext repositoryContext() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        MavenExecutionContextView.view(ctx)
          .setPomCache(new InMemoryMavenPomCache())
          .setRepositories(List.of(REPOSITORY));
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            String path = request.getUrl().getPath();
            String body;
            if (path.endsWith("/com/example/my-lib/maven-metadata.xml")) {
                body = """
                  <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <versioning>
                      <versions>
                        <version>1.0.0</version>
                        <version>2.0.0</version>
                        <version>2.0.0-jre</version>
                        <version>2.5.0-android</version>
                        <version>3.0.0-RC1</version>
                      </versions>
                    </versioning>
                  </metadata>
                  """;
            } else if (path.contains("/com/example/my-lib/") && path.endsWith(".pom")) {
                String version = path.substring(path.lastIndexOf("/my-lib-") + "/my-lib-".length(), path.length() - 4);
                if (!List.of("1.0.0", "2.0.0", "2.0.0-jre", "2.5.0-android", "3.0.0-RC1").contains(version)) {
                    return new MockHttpSender(404).send(request);
                }
                body = """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-lib</artifactId>
                    <version>%s</version>
                  </project>
                  """.formatted(version);
            } else {
                return new MockHttpSender(404).send(request);
            }
            return new MockHttpSender(() -> new ByteArrayInputStream(body.getBytes(UTF_8))).send(request);
        });
        return ctx;
    }
}
