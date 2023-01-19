/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.internal;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SuppressWarnings({"NullableProblems", "HttpUrlsUsage"})
class MavenPomDownloaderTest {
    private final ExecutionContext ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
      .setHttpSender(new HttpUrlConnectionSender(Duration.ofMillis(100), Duration.ofMillis(100)));

    private void mockServer(Integer responseCode, Consumer<MockWebServer> block) {
        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest recordedRequest) {
                    return new MockResponse().setResponseCode(responseCode).setBody("");
                }
            });
            mockRepo.start();
            block.accept(mockRepo);
            assertThat(mockRepo.getRequestCount())
              .as("The mock repository received no requests. The test is not using it.")
              .isGreaterThan(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Disabled("Flaky on CI")
    @Test
    void normalizeOssSnapshots() {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        MavenRepository oss = downloader.normalizeRepository(
          MavenRepository.builder().id("oss").uri("https://oss.sonatype.org/content/repositories/snapshots").build(),
          null);

        assertThat(oss).isNotNull();
        assertThat(oss.getUri()).isEqualTo("https://oss.sonatype.org/content/repositories/snapshots");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 400})
    void normalizeAcceptErrorStatuses(Integer status) {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        mockServer(status, mockRepo -> {
            var originalRepo = MavenRepository.builder()
              .id("id")
              .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
              .build();
            var normalizedRepo = downloader.normalizeRepository(originalRepo, null);
            assertThat(normalizedRepo).isEqualTo(originalRepo);
        });
    }

    @Test
    void normalizeRejectConnectException() {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        var normalizedRepository = downloader.normalizeRepository(
          MavenRepository.builder().id("id").uri("https//localhost").build(),
          null
        );
        assertThat(normalizedRepository).isEqualTo(null);
    }

    @Test
    void invalidArtifact() {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
        mockServer(500,
          repo1 -> mockServer(400, repo2 -> {
              var repositories = List.of(
                MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven".formatted(repo1.getHostName(), repo1.getPort()))
                  .build(),
                MavenRepository.builder()
                  .id("id2")
                  .uri("http://%s:%d/maven".formatted(repo2.getHostName(), repo2.getPort()))
                  .build()
              );

              assertThatThrownBy(() -> downloader.download(gav, null, null, repositories))
                .isInstanceOf(MavenDownloadingException.class)
                .hasMessageContaining("http://%s:%d/maven".formatted(repo1.getHostName(), repo1.getPort()))
                .hasMessageContaining("http://%s:%d/maven".formatted(repo2.getHostName(), repo2.getPort()));
          })
        );
    }

    @Test
    void usesAnonymousRequestIfRepositoryRejectsCredentials() {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest recordedRequest) {
                    return recordedRequest.getHeaders().get("Authorization") != null ?
                      new MockResponse().setResponseCode(401).setBody("") :
                      new MockResponse().setResponseCode(200).setBody(
                        //language=xml
                        """
                        <project>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-dataflow-build</artifactId>
                            <version>2.10.0-SNAPSHOT</version>
                        </project>
                        """);
                }
            });
            mockRepo.start();
            var repositories = List.of(MavenRepository.builder()
              .id("id")
              .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
              .username("user")
              .password("pass")
              .build());

            assertDoesNotThrow(() -> downloader.download(gav, null, null, repositories));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void usesAuthenticationIfRepositoryHasCredentials() {
        var downloader = new MavenPomDownloader(emptyMap(), ctx);
        var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest recordedRequest) {
                    return recordedRequest.getHeaders().get("Authorization") != null ?
                      new MockResponse().setResponseCode(200).setBody(
                        //language=xml
                        """
                        <project>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-dataflow-build</artifactId>
                            <version>2.10.0-SNAPSHOT</version>
                        </project>
                        """) :
                      new MockResponse().setResponseCode(401).setBody("");
                }
            });
            mockRepo.start();
            var repositories = List.of(MavenRepository.builder()
              .id("id")
              .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
              .username("user")
              .password("pass")
              .build());

            assertDoesNotThrow(() -> downloader.download(gav, null, null, repositories));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Disabled
    void dontFetchSnapshotsFromReleaseRepos() {
        try (MockWebServer snapshotRepo = new MockWebServer();
             MockWebServer releaseRepo = new MockWebServer()) {
            snapshotRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    MockResponse response = new MockResponse().setResponseCode(200);
                    if (request.getPath() != null && request.getPath().contains("maven-metadata")) {
                        response.setBody(
                          //language=xml
                          """
                                <metadata modelVersion="1.1.0">
                                  <groupId>org.springframework.cloud</groupId>
                                  <artifactId>spring-cloud-dataflow-build</artifactId>
                                  <version>2.10.0-SNAPSHOT</version>
                                  <versioning>
                                    <snapshot>
                                      <timestamp>20220201.001946</timestamp>
                                      <buildNumber>85</buildNumber>
                                    </snapshot>
                                    <lastUpdated>20220201001950</lastUpdated>
                                    <snapshotVersions>
                                      <snapshotVersion>
                                        <extension>pom</extension>
                                        <value>2.10.0-20220201.001946-85</value>
                                        <updated>20220201001946</updated>
                                      </snapshotVersion>
                                    </snapshotVersions>
                                  </versioning>
                                </metadata>
                            """
                        );
                    } else if (request.getPath() != null && request.getPath().endsWith(".pom")) {
                        response.setBody(
                          //language=xml
                          """
                                <project>
                                    <groupId>org.springframework.cloud</groupId>
                                    <artifactId>spring-cloud-dataflow-build</artifactId>
                                    <version>2.10.0-SNAPSHOT</version>
                                </project>
                            """
                        );
                    }
                    return response;
                }
            });

            var metadataPaths = new AtomicInteger(0);
            releaseRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    MockResponse response = new MockResponse().setResponseCode(404);
                    if (request.getPath() != null && request.getPath().contains("maven-metadata")) {
                        metadataPaths.incrementAndGet();
                    }
                    return response;
                }
            });

            releaseRepo.start();
            snapshotRepo.start();

            MavenParser.builder().build().parse(ctx,
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    
                        <groupId>org.openrewrite.test</groupId>
                        <artifactId>foo</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        
                        <repositories>
                          <repository>
                            <id>snapshot</id>
                            <snapshots>
                              <enabled>true</enabled>
                            </snapshots>
                            <url>http://%s:%d</url>
                          </repository>
                          <repository>
                            <id>release</id>
                            <snapshots>
                              <enabled>false</enabled>
                            </snapshots>
                            <url>http://%s:%d</url>
                          </repository>
                        </repositories>
                        
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.cloud</groupId>
                                <artifactId>spring-cloud-dataflow-build</artifactId>
                                <version>2.10.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                    </project>
                """.formatted(snapshotRepo.getHostName(), snapshotRepo.getPort(), releaseRepo.getHostName(), releaseRepo.getPort())
            );

            assertThat(snapshotRepo.getRequestCount()).isGreaterThan(1);
            assertThat(metadataPaths.get()).isEqualTo(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deriveMetaDataFromFileRepository(@TempDir Path repoPath) throws IOException, MavenDownloadingException {
        Path fred = repoPath.resolve("fred/fred");
        Files.createDirectories(fred.resolve("1.0.0"));
        Files.createDirectories(fred.resolve("1.1.0"));
        Files.createDirectories(fred.resolve("2.0.0"));

        MavenRepository repository = MavenRepository.builder()
          .id("file-based")
          .uri(repoPath.toUri().toString())
          .knownToExist(true)
          .deriveMetadataIfMissing(true)
          .build();
        MavenMetadata metaData  = new MavenPomDownloader(emptyMap(), new InMemoryExecutionContext())
          .downloadMetadata(new GroupArtifact("fred", "fred"), null, List.of(repository));
        assertThat(metaData.getVersioning().getVersions()).hasSize(3).containsAll(Arrays.asList("1.0.0", "1.1.0", "2.0.0"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void mergeMetadata() throws IOException {
        @Language("xml") String metadata1 = """
              <metadata>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot</artifactId>
                  <versioning>
                      <versions>
                          <version>2.3.3</version>
                          <version>2.4.1</version>
                          <version>2.4.2</version>
                      </versions>
                      <snapshot>
                          <timestamp>20220927.033510</timestamp>
                          <buildNumber>223</buildNumber>
                      </snapshot>
                      <snapshotVersions>
                          <snapshotVersion>
                              <extension>pom.asc</extension>
                              <value>0.1.0-20220927.033510-223</value>
                              <updated>20220927033510</updated>
                          </snapshotVersion>
                      </snapshotVersions>
                  </versioning>
              </metadata>
          """;

        @Language("xml") String metadata2 = """
              <metadata modelVersion="1.1.0">
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot</artifactId>
                  <versioning>
                      <versions>
                          <version>2.3.2</version>
                          <version>2.3.3</version>
                      </versions>
                      <snapshot>
                          <timestamp>20210115.042754</timestamp>
                          <buildNumber>180</buildNumber>
                      </snapshot>
                      <snapshotVersions>
                          <snapshotVersion>
                              <extension>pom.asc</extension>
                              <value>0.1.0-20210115.042754-180</value>
                              <updated>20210115042754</updated>
                          </snapshotVersion>
                      </snapshotVersions>
                  </versioning>
              </metadata>
          """;

        var m1 = MavenMetadata.parse(metadata1.getBytes());
        var m2 = MavenMetadata.parse(metadata2.getBytes());

        var merged = new MavenPomDownloader(emptyMap(), new InMemoryExecutionContext()).mergeMetadata(m1, m2);

        assertThat(merged.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20220927.033510");
        assertThat(merged.getVersioning().getSnapshot().getBuildNumber()).isEqualTo("223");
        assertThat(merged.getVersioning().getVersions()).hasSize(4).contains("2.3.2", "2.3.3", "2.4.1", "2.4.2");
        assertThat(merged.getVersioning().getSnapshotVersions()).hasSize(2);
        assertThat(merged.getVersioning().getSnapshotVersions().get(0).getExtension()).isNotNull();
        assertThat(merged.getVersioning().getSnapshotVersions().get(0).getValue()).isNotNull();
        assertThat(merged.getVersioning().getSnapshotVersions().get(0).getUpdated()).isNotNull();
    }
}
