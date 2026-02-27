/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.utilities;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MavenArtifactDownloaderTest {

    @Test
    void downloadDependencies(@TempDir Path tempDir) {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir);
        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
          artifactCache, null, t -> ctx.getOnError().accept(t));
        ResolvedGroupArtifactVersion recipeGav = new ResolvedGroupArtifactVersion(
          "https://repo1.maven.org/maven2",
          "org.openrewrite.recipe",
          "rewrite-testing-frameworks",
          "1.6.0", null);

        MavenParser mavenParser = MavenParser.builder().build();
        SourceFile parsed = mavenParser.parse(ctx,
                """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>maven-downloader-test</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(recipeGav.getGroupId(), recipeGav.getArtifactId(), recipeGav.getVersion()).formatted()
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

        MavenResolutionResult mavenModel = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(mavenModel.getDependencies()).isNotEmpty();

        List<ResolvedDependency> runtimeDependencies = mavenModel.getDependencies().get(Scope.Runtime);
        for (ResolvedDependency runtimeDependency : runtimeDependencies) {
            if (!("bom".equals(runtimeDependency.getType()))) {
                assertNotNull(
                  downloader.downloadArtifact(runtimeDependency),
                        "%s:%s:%s:%s failed to download".formatted(
                                runtimeDependency.getGroupId(),
                                runtimeDependency.getArtifactId(),
                                runtimeDependency.getVersion(),
                                runtimeDependency.getType()));
            }
        }
    }

    @Test
    void downloadDependenciesWithClassifier(@TempDir Path tempDir) {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir);
        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
          artifactCache, null, t -> ctx.getOnError().accept(t));

        MavenParser mavenParser = MavenParser.builder().build();
        SourceFile parsed = mavenParser.parse(ctx,
          //language=xml
          """
            <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>maven-downloader-test</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>net.sf.json-lib</groupId>
                        <artifactId>json-lib</artifactId>
                        <version>2.4</version>
                        <classifier>jdk15</classifier>
                    </dependency>
                </dependencies>
            </project>
            """
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

        MavenResolutionResult mavenModel = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(mavenModel.getDependencies()).isNotEmpty();

        List<ResolvedDependency> runtimeDependencies = mavenModel.getDependencies().get(Scope.Runtime);
        for (ResolvedDependency runtimeDependency : runtimeDependencies) {
            if (!("bom".equals(runtimeDependency.getType()))) {
                assertNotNull(
                  downloader.downloadArtifact(runtimeDependency),
                        "%s:%s:%s:%s failed to download".formatted(
                                runtimeDependency.getGroupId(),
                                runtimeDependency.getArtifactId(),
                                runtimeDependency.getVersion(),
                                runtimeDependency.getType()));
            }
        }
    }

    @Test
    void fallsBackToAnonymousWhenCredentialsRejected(@TempDir Path tempDir) throws Exception {
        byte[] jarBytes = {0x50, 0x4B, 0x03, 0x04}; // minimal ZIP magic bytes

        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    if (request.getHeader("Authorization") != null) {
                        return new MockResponse().setResponseCode(403);
                    }
                    return new MockResponse().setResponseCode(200)
                      .setBody(new okio.Buffer().write(jarBytes));
                }
            });
            mockRepo.start();

            String repoUrl = "http://" + mockRepo.getHostName() + ":" + mockRepo.getPort();
            MavenSettings settings = MavenSettings.parse(new Parser.Input(
              Path.of("settings.xml"), () -> new ByteArrayInputStream(
              //language=xml
              """
                <settings>
                    <servers>
                        <server>
                            <id>mock-repo</id>
                            <username>baduser</username>
                            <password>badpass</password>
                        </server>
                    </servers>
                </settings>
                """.getBytes())), new InMemoryExecutionContext());

            MavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir);
            AtomicReference<Throwable> error = new AtomicReference<>();
            MavenArtifactDownloader downloader = new MavenArtifactDownloader(
              artifactCache, settings, error::set);

            MavenRepository repo = new MavenRepository(
              "mock-repo", repoUrl, "true", "false", true, null, null, null, false);
            GroupArtifactVersion gav = new GroupArtifactVersion("com.example", "test-lib", "1.0.0");
            ResolvedDependency dep = ResolvedDependency.builder()
              .repository(repo)
              .gav(new ResolvedGroupArtifactVersion(repoUrl, gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), null))
              .requested(Dependency.builder().gav(gav).build())
              .build();

            Path artifact = downloader.downloadArtifact(dep);

            assertThat(artifact).isNotNull();
            assertThat(error.get()).isNull();
            assertThat(mockRepo.getRequestCount()).isGreaterThanOrEqualTo(2);
        }
    }
}
