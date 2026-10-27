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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.test.MockHttpSender;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openrewrite.maven.Assertions.pomXml;

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
        String before = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.example</groupId>
            <artifactId>app</artifactId>
            <version>1.0.0</version>
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-lib</artifactId>
                <version>1.0.0</version>
              </dependency>
            </dependencies>
          </project>
          """;
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.example", "my-lib", selector, versionPattern, null, null))
            .executionContext(repositoryContext())
            .cycles(2).expectedCyclesThatMakeChanges(1),
          pomXml(before, before.replace("<artifactId>my-lib</artifactId>\n      <version>1.0.0</version>",
            "<artifactId>my-lib</artifactId>\n      <version>" + expected + "</version>"))
        );
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
