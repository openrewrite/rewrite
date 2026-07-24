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

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.*;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenMetadataCacheEntry;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.http.OkHttpSender;
import org.openrewrite.maven.tree.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;

@SuppressWarnings({"HttpUrlsUsage"})
class MavenPomDownloaderTest implements RewriteTest {

    @Test
    void ossSonatype() {
        var ctx = new InMemoryExecutionContext();
        MavenRepository ossSonatype = MavenRepository.builder()
          .id("oss")
          .uri("https://central.sonatype.com/repository/maven-snapshots/")
          .snapshots(true)
          .build();
        MavenRepository repo = new MavenPomDownloader(ctx).normalizeRepository(ossSonatype,
          MavenExecutionContextView.view(ctx), null);
        assertThat(repo).isNotNull().extracting((MavenRepository::getUri)).isEqualTo(ossSonatype.getUri());
    }

    @CsvSource(textBlock = """
      https://repo1.maven.org/maven2/, https://repo1.maven.org/maven2/
      https://repo1.maven.org/maven2, https://repo1.maven.org/maven2/
      http://repo1.maven.org/maven2/, https://repo1.maven.org/maven2/

      https://central.sonatype.com/repository/maven-snapshots/, https://central.sonatype.com/repository/maven-snapshots/
      https://artifactory.moderne.ninja/artifactory/moderne-public/, https://artifactory.moderne.ninja/artifactory/moderne-public/
      https://repo.maven.apache.org/maven2/, https://repo.maven.apache.org/maven2/
      https://jitpack.io/, https://jitpack.io/
      """)
    @ParameterizedTest
    void normalizeRepository(String originalUrl, String expectedUrl) throws Throwable {
        var downloader = new MavenPomDownloader(new InMemoryExecutionContext());
        var repository = new MavenRepository("id", originalUrl, null, null, null, null, null);
        MavenRepository normalized = downloader.normalizeRepository(repository);
        assertThat(normalized).isNotNull();
        assertThat(normalized.getUri()).isEqualTo(expectedUrl);
    }

    /**
     * Documented in <a href="https://maven.apache.org/guides/mini/guide-multiple-repositories.html">Maven's guide</a>.
     * Effective Maven settings take precedence over the local effective build POM.
     */
    @Test
    void repositoryOrder() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings>
              <profiles>
                <profile>
                    <id>customize-repos</id>
                    <repositories>
                        <repository>
                            <id>settings-provided</id>
                            <url>https://repo.clojars.org</url>
                        </repository>
                    </repositories>
                </profile>
              </profiles>
              <activeProfiles>
                <activeProfile>customize-repos</activeProfile>
              </activeProfiles>
            </settings>
            """
        ), ctx));

        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>org.openrewrite.test</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <repositories>
                      <repository>
                          <id>local-provided</id>
                          <url>https://oss.sonatype.org/content/repositories/releases</url>
                      </repository>
                  </repositories>
              </project>
              """,
            spec -> spec.beforeRecipe(pom -> {
                MavenResolutionResult result = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(StreamSupport.stream(new MavenPomDownloader(ctx).distinctNormalizedRepositories(result.getPom().getRepositories(), result.getPom(), null).spliterator(), false)
                  .map(MavenRepository::getId))
                  .containsExactly("settings-provided", "local-provided");
            }))
        );
    }

    @Nested
    class WithNativeHttpURLConnectionAndTLS {
        private final ExecutionContext ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(new HttpUrlConnectionSender(Duration.ofMillis(250), Duration.ofMillis(250)));

        @Issue("https://github.com/openrewrite/rewrite/issues/3908")
        @Test
        void centralIdOverridesDefaultRepository() {
            var ctx = MavenExecutionContextView.view(this.ctx);
            var centralOverride = new MavenRepository("repo", "https://google.com/definitelydoesnotexist/", null, null, true, null, null, null, null);
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            try {
                downloader.download(new GroupArtifactVersion("org.openrewrite", "nonexistent", "7.0.0"), null, null, List.of(centralOverride));
                fail();
            } catch (MavenDownloadingException ignore) {
            }
        }

        @Test
        void unreachableHostProbedOncePerRun() {
            var ctx = MavenExecutionContextView.view(this.ctx);
            // Nothing listens on port 1, so connections are refused immediately.
            String deadUri = "http://localhost:1/repo/";

            List<String> probed = new ArrayList<>();
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void repositoryAccessFailed(String uri, Throwable t) {
                    probed.add(uri);
                }
            });

            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            // The same dead host declared under two different repository ids — as happens when
            // several transitive POMs each declare the same (dead) repository. The per-repository
            // normalization cache does not dedupe these by host, so without a host-level circuit
            // breaker each one is re-probed (here, ~once; in a real run, once per artifact).
            var repoA = MavenRepository.builder().id("a").uri(deadUri).build();
            var repoB = MavenRepository.builder().id("b").uri(deadUri).build();

            assertThat(downloader.normalizeRepository(repoA, ctx, null)).isNull();
            assertThat(downloader.normalizeRepository(repoB, ctx, null)).isNull();

            // The unreachable host must be probed only once; the second repository is skipped.
            assertThat(probed).containsExactly(deadUri);
        }

        @Test
        void listenerRecordsRepository() {
            var ctx = MavenExecutionContextView.view(this.ctx);
            // Avoid actually trying to reach the made-up https://internalartifactrepository.yourorg.com
            for (MavenRepository repository : ctx.getRepositories()) {
                repository.setKnownToExist(true);
            }

            var nonexistentRepo = new MavenRepository("repo", "http://internalartifactrepository.yourorg.com", null, null, true, null, null, null, null);
            List<String> attemptedUris = new ArrayList<>();
            List<MavenRepository> discoveredRepositories = new ArrayList<>();
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void downloadError(GroupArtifactVersion gav, List<String> uris, @Nullable Pom containing) {
                    attemptedUris.addAll(uris);
                }

                @Override
                public void repository(MavenRepository mavenRepository, @Nullable ResolvedPom containing) {
                    discoveredRepositories.add(mavenRepository);
                }
            });

            try {
                new MavenPomDownloader(ctx)
                  .download(new GroupArtifactVersion("org.openrewrite", "rewrite-core", "7.0.0"), null, null, singletonList(nonexistentRepo));
            } catch (Exception e) {
                // not expected to succeed
            }
            assertThat(attemptedUris)
              .containsExactly("http://internalartifactrepository.yourorg.com/org/openrewrite/rewrite-core/7.0.0/rewrite-core-7.0.0.pom");
            assertThat(discoveredRepositories)
              .containsExactly(nonexistentRepo);
        }

        @Test
        void onlyAccessRequiredRepositories() throws Exception {
            var ctx = MavenExecutionContextView.view(this.ctx);
            ctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));
            // Avoid actually trying to reach the made-up https://internalartifactrepository.yourorg.com
            for (MavenRepository repository : ctx.getRepositories()) {
                repository.setKnownToExist(true);
            }

            var nonExistentRepo = new MavenRepository("repo", "https://definitelydoesnotexist2.xyz/", null, null, false, null, null, null, null);
            List<String> attemptedUris = new ArrayList<>();
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void repositoryAccessFailed(String uri, Throwable e) {
                    attemptedUris.add(uri);
                }
            });

            new MavenPomDownloader(ctx)
              .download(new GroupArtifactVersion("org.openrewrite", "rewrite-core", "7.0.0"), null, null, List.of(MAVEN_CENTRAL, nonExistentRepo));
            assertThat(attemptedUris).isEmpty();
        }

        @Test
        void listenerRecordsFailedRepositoryAccess() {
            var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
            // Avoid actually trying to reach a made-up URL
            String httpUrl = "http://%s.com".formatted(UUID.randomUUID());
            var nonexistentRepo = new MavenRepository("repo", httpUrl, null, null, false, null, null, null, null);
            Map<String, Throwable> attemptedUris = new HashMap<>();
            List<MavenRepository> discoveredRepositories = new ArrayList<>();
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void repositoryAccessFailed(String uri, Throwable e) {
                    attemptedUris.put(uri, e);
                }
            });

            try {
                new MavenPomDownloader(ctx)
                  .download(new GroupArtifactVersion("org.openrewrite", "rewrite-core", "7.0.0"), null, null, singletonList(nonexistentRepo));
            } catch (Exception e) {
                // not expected to succeed
            }
            assertThat(attemptedUris).isNotEmpty();
            assertThat(attemptedUris.get(httpUrl)).isInstanceOf(UnknownHostException.class);
            assertThat(discoveredRepositories).isEmpty();
        }

        @Test
        void mirrorsOverrideRepositoriesInPom() {
            var ctx = MavenExecutionContextView.view(this.ctx);
            // normalizeRepository probes the mirror over HTTP; the class-level 250ms timeout
            // is too aggressive under suite load and causes the probe to return null.
            HttpSenderExecutionContextView.view(this.ctx).setHttpSender(new HttpUrlConnectionSender());
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                  <mirrors>
                    <mirror>
                      <id>mirror</id>
                      <url>https://artifactory.moderne.ninja/artifactory/moderne-cache</url>
                      <mirrorOf>*</mirrorOf>
                    </mirror>
                  </mirrors>
                </settings>
                """
            ), ctx));

            Path pomPath = Path.of("pom.xml");
            Pom pom = Pom.builder()
              .sourcePath(pomPath)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "org.openrewrite", "rewrite-core", "7.0.0", null))
              .build();
            ResolvedPom resolvedPom = ResolvedPom.builder()
              .requested(pom)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .repositories(singletonList(MAVEN_CENTRAL))
              .build();

            Map<Path, Pom> pomsByPath = new HashMap<>();
            pomsByPath.put(pomPath, pom);

            var mpd = new MavenPomDownloader(pomsByPath, ctx);
            MavenRepository normalized = mpd.normalizeRepository(
              MavenRepository.builder().id("whatever").uri("${REPO_URL}").build(),
              ctx,
              resolvedPom
            );
            assertThat(normalized)
              .extracting(MavenRepository::getUri)
              .isEqualTo("https://artifactory.moderne.ninja/artifactory/moderne-cache/");
        }

        @Disabled("Flaky on CI")
        @Test
        void normalizeOssSnapshots() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            MavenRepository oss = downloader.normalizeRepository(
              MavenRepository.builder().id("oss").uri("https://central.sonatype.com/repository/maven-snapshots").build(),
              MavenExecutionContextView.view(ctx), null);

            assertThat(oss).isNotNull();
            assertThat(oss.getUri()).isEqualTo("https://central.sonatype.com/repository/maven-snapshots/");
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/3141")
        @ParameterizedTest
        @ValueSource(strings = {"http://0.0.0.0", "https://0.0.0.0", "0.0.0.0:443"})
        void skipBlockedRepository(String url) {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            MavenRepository oss = downloader.normalizeRepository(
              MavenRepository.builder().id("myRepo").uri(url).build(),
              MavenExecutionContextView.view(ctx), null);

            assertThat(oss).isNull();
        }

        @Test
        void retryConnectException() throws Exception {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            try (var server = new MockWebServer()) {
                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                server.enqueue(new MockResponse().setResponseCode(200).setBody("body"));
                var body = new String(downloader.sendRequest(new HttpSender.Request(server.url("/test").url(), "request".getBytes(), HttpSender.Method.GET, Map.of(), null, null)));
                assertThat(body).isEqualTo("body");
                assertThat(server.getRequestCount()).isEqualTo(2);
                server.shutdown();
            }
        }

        @Test
        void normalizeRejectConnectException() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var normalizedRepository = downloader.normalizeRepository(
              MavenRepository.builder().id("id").uri("https//localhost").build(),
              MavenExecutionContextView.view(ctx), null);
            assertThat(normalizedRepository).isEqualTo(null);
        }

        @Test
        void useHttpWhenHttpsFails() throws Exception {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            try (var mockRepo = new MockWebServer()) {
                // Use a Dispatcher instead of enqueue to avoid flakiness: normalizeRepository()
                // probes HTTPS first, and TLS bytes sent to this HTTP-only server can sometimes
                // parse as valid HTTP, consuming an enqueued response before the real HTTP request.
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        return new MockResponse().setResponseCode(200).setBody("body");
                    }
                });
                mockRepo.start();
                var httpRepo = MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven/".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .build();

                var normalizedRepository = downloader.normalizeRepository(httpRepo, MavenExecutionContextView.view(ctx), null);

                assertThat(normalizedRepository).isEqualTo(httpRepo);
            }
        }

        @Test
        void dontFetchSnapshotsFromReleaseRepos() throws Exception {
            try (var snapshotRepo = new MockWebServer();
                 var releaseRepo = new MockWebServer()) {
                snapshotRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        MockResponse response = new MockResponse().setResponseCode(200);
                        if (request.getPath() != null && request.getPath().contains("maven-metadata")) {
                            //language=xml
                            response.setBody(
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
                            //language=xml
                            response.setBody(
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
                  //language=xml
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
            }
        }

        @Test
        void datedSnapshotVersionIncludesSnapshotRepositories() throws Exception {
            try (var snapshotRepo = new MockWebServer()) {
                snapshotRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        MockResponse response = new MockResponse().setResponseCode(200);
                        if (request.getPath() != null && request.getPath().endsWith(".pom")) {
                            //language=xml
                            response.setBody(
                              """
                                <project>
                                    <groupId>com.example</groupId>
                                    <artifactId>my-lib</artifactId>
                                    <version>3.28.0-SNAPSHOT</version>
                                </project>
                                """
                            );
                        }
                        return response;
                    }
                });

                snapshotRepo.start();

                var downloader = new MavenPomDownloader(emptyMap(), ctx);
                var snapshotRepoModel = MavenRepository.builder()
                  .id("snapshots")
                  .uri("http://%s:%d".formatted(snapshotRepo.getHostName(), snapshotRepo.getPort()))
                  .releases("false")
                  .snapshots(true)
                  .build();

                // A dated snapshot version should be recognized as a snapshot so that
                // snapshot-only repositories are not excluded.
                Pom pom = downloader.download(
                  new GroupArtifactVersion("com.example", "my-lib", "3.28.0-20260220.175218-20"),
                  null, null, List.of(snapshotRepoModel));

                assertThat(pom).isNotNull();
                assertThat(snapshotRepo.getRequestCount()).isGreaterThan(0);
            }
        }

        @Issue("https://github.com/openrewrite/rewrite-maven-plugin/issues/862")
        @Test
        void fetchSnapshotWithCorrectClassifier() throws Exception {
            try (var snapshotServer = new MockWebServer()) {
                snapshotServer.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        MockResponse response = new MockResponse().setResponseCode(200);
                        if (request.getPath() != null && request.getPath().contains("maven-metadata")) {
                            //language=xml
                            response.setBody(
                              """
                                <metadata modelVersion="1.1.0">
                                  <groupId>com.some</groupId>
                                  <artifactId>an-artifact</artifactId>
                                  <version>10.5.0-SNAPSHOT</version>
                                  <versioning>
                                    <snapshot>
                                      <timestamp>20250113.114247</timestamp>
                                      <buildNumber>36</buildNumber>
                                    </snapshot>
                                    <lastUpdated>20250113114247</lastUpdated>
                                    <snapshotVersions>
                                      <snapshotVersion>
                                        <classifier>javadoc</classifier>
                                        <extension>jar</extension>
                                        <value>10.5.0-20250113.114247-36</value>
                                        <updated>20250113114247</updated>
                                      </snapshotVersion>
                                      <snapshotVersion>
                                        <classifier>tests</classifier>
                                        <extension>jar</extension>
                                        <value>10.5.0-20250113.114244-35</value>
                                        <updated>20250113114244</updated>
                                      </snapshotVersion>
                                      <snapshotVersion>
                                        <classifier>sources</classifier>
                                        <extension>jar</extension>
                                        <value>10.5.0-20250113.114242-34</value>
                                        <updated>20250113114242</updated>
                                      </snapshotVersion>
                                      <snapshotVersion>
                                        <extension>jar</extension>
                                        <value>10.5.0-20250113.114227-33</value>
                                        <updated>20250113114227</updated>
                                      </snapshotVersion>
                                      <snapshotVersion>
                                        <extension>pom</extension>
                                        <value>10.5.0-20250113.114227-33</value>
                                        <updated>20250113114227</updated>
                                      </snapshotVersion>
                                    </snapshotVersions>
                                  </versioning>
                                </metadata>
                                """
                            );
                        } else if (request.getPath() != null && request.getPath().endsWith(".pom")) {
                            //language=xml
                            response.setBody(
                              """
                                <project>
                                     <groupId>com.some</groupId>
                                     <artifactId>an-artifact</artifactId>
                                     <version>10.5.0-SNAPSHOT</version>
                                </project>
                                """
                            );
                        }
                        return response;
                    }
                });

                snapshotServer.start();

                //language=xml
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
                        </repositories>
                        <dependencies>
                            <dependency>
                                 <groupId>com.some</groupId>
                                 <artifactId>an-artifact</artifactId>
                                 <version>10.5.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """.formatted(snapshotServer.getHostName(), snapshotServer.getPort())
                );

                MavenRepository snapshotRepo = MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven/".formatted(snapshotServer.getHostName(), snapshotServer.getPort()))
                  .build();

                var gav = new GroupArtifactVersion("com.some", "an-artifact", "10.5.0-SNAPSHOT");
                var mavenPomDownloader = new MavenPomDownloader(emptyMap(), ctx);

                var pomPath = Path.of("pom.xml");
                var pom = Pom.builder()
                  .sourcePath(pomPath)
                  .repository(snapshotRepo)
                  .properties(singletonMap("REPO_URL", snapshotRepo.getUri()))
                  .gav(gav.asResolved().withRepository("${REPO_URL}"))
                  .build();
                var resolvedPom = ResolvedPom.builder()
                  .requested(pom)
                  .properties(singletonMap("REPO_URL", snapshotRepo.getUri()))
                  .repositories(singletonList(snapshotRepo))
                  .build();

                // Ensure that classifier 'javadoc', 'tests' and 'sources' are not used
                var downloadedPom = mavenPomDownloader.download(gav, null, resolvedPom, List.of(snapshotRepo));
                assertThat(downloadedPom).returns("10.5.0-20250113.114227-33", Pom::getDatedSnapshotVersion);
            }
        }

        @Test
        void deriveMetaDataFromFileRepository(@TempDir Path repoPath) throws Exception {
            Path fred = repoPath.resolve("fred/fred");

            for (String version : List.of("1.0.0", "1.1.0", "2.0.0")) {
                Path versionPath = fred.resolve(version);
                Files.createDirectories(versionPath);
                Files.writeString(versionPath.resolve("fred-" + version + ".pom"), "");
            }

            MavenRepository repository = MavenRepository.builder()
              .id("file-based")
              .uri(repoPath.toUri().toString())
              .knownToExist(true)
              .deriveMetadataIfMissing(true)
              .build();
            MavenMetadata metaData = new MavenPomDownloader(emptyMap(), ctx)
              .downloadMetadata(new GroupArtifact("fred", "fred"), null, List.of(repository));
            assertThat(metaData.getVersioning().getVersions()).hasSize(3).containsAll(List.of("1.0.0", "1.1.0", "2.0.0"));
        }

        @Test
        void downloadMetadataFromFileRepoWithNonAsciiPath(@TempDir Path repoPath) throws Exception {
            // Reproduce "Bad escape" error when file:// repo path contains non-ASCII chars
            // like German umlauts (e.g. a username like "müller")
            Path repoWithUmlaut = repoPath.resolve("m\u00fcller/.m2/repository");
            Path artifactDir = repoWithUmlaut.resolve("com/example/my-lib");
            for (String version : List.of("1.0.0", "2.0.0")) {
                Path versionPath = artifactDir.resolve(version);
                Files.createDirectories(versionPath);
                Files.writeString(versionPath.resolve("my-lib-" + version + ".pom"),
                  """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example</groupId>
                      <artifactId>my-lib</artifactId>
                      <version>%s</version>
                    </project>
                    """.formatted(version));
            }
            Files.writeString(artifactDir.resolve("maven-metadata-local.xml"),
              """
                <metadata>
                  <groupId>com.example</groupId>
                  <artifactId>my-lib</artifactId>
                  <versioning>
                    <versions>
                      <version>1.0.0</version>
                      <version>2.0.0</version>
                    </versions>
                  </versioning>
                </metadata>
                """);

            // Use a URI with properly encoded umlauts (as Path.toUri() produces)
            MavenRepository repository = MavenRepository.builder()
              .id("local")
              .uri(repoWithUmlaut.toUri().toString())
              .knownToExist(true)
              .build();
            MavenMetadata metaData = new MavenPomDownloader(emptyMap(), ctx)
              .downloadMetadata(new GroupArtifact("com.example", "my-lib"), null, List.of(repository));
            assertThat(metaData.getVersioning().getVersions()).containsExactly("1.0.0", "2.0.0");

            // Also test with raw (unencoded) umlauts in the URI, which is what happens
            // when the repo URL comes from Maven settings XML with non-ASCII path chars
            String absPath = repoWithUmlaut.toAbsolutePath().toString().replace('\\', '/');
            String rawUri = "file://" + (absPath.startsWith("/") ? "" : "/") + absPath + "/";
            MavenRepository rawRepo = MavenRepository.builder()
              .id("local-raw")
              .uri(rawUri)
              .knownToExist(true)
              .build();
            MavenMetadata rawMetaData = new MavenPomDownloader(emptyMap(), ctx)
              .downloadMetadata(new GroupArtifact("com.example", "my-lib"), null, List.of(rawRepo));
            assertThat(rawMetaData.getVersioning().getVersions()).containsExactly("1.0.0", "2.0.0");
        }

        @CsvSource({
          // Already valid — idempotent
          "'file:///tmp/repo/',                                          'file:///tmp/repo/'",
          "'file:///tmp/repo',                                           'file:///tmp/repo'",
          // Already percent-encoded — idempotent
          "'file:///tmp/m%C3%BCller/repo/',                              'file:///tmp/m%C3%BCller/repo/'",
          // Raw non-ASCII characters get encoded
          "'file:///tmp/müller/repo/',                                   'file:///tmp/m%C3%BCller/repo/'",
          // Spaces get encoded
          "'file:///tmp/has space/repo/',                                'file:///tmp/has%20space/repo/'",
          // Backslashes normalized to forward slashes
          "'file:///tmp/path\\with\\backslashes/repo/',                  'file:///tmp/path/with/backslashes/repo/'",
          // Windows drive letter preserved
          "'file:///C:/Users/test/.m2/repository/',                      'file:///C:/Users/test/.m2/repository/'",
          // Malformed Windows URI (two slashes + backslashes) normalized
          "'file://C:\\Users\\test\\.m2\\repository/',                   'file:///C:/Users/test/.m2/repository/'",
          // Malformed Windows URI with non-ASCII
          "'file://C:\\Users\\müller\\.m2\\repository/',                 'file:///C:/Users/m%C3%BCller/.m2/repository/'",
        })
        @ParameterizedTest
        void normalizeFileUri(String input, String expected) {
            String normalized = MavenPomDownloader.normalizeFileUri(input);
            assertThat(normalized).isEqualTo(expected);
            assertDoesNotThrow(() -> Paths.get(URI.create(normalized)));
        }


        @Test
        void deriveMetaDataFromHtmlBasedRepository() {
            MavenRepository repository = MavenRepository.builder()
              .id("html-based")
              .uri("https://central.sonatype.com/repository/maven-snapshots")
              .knownToExist(true)
              .deriveMetadataIfMissing(true)
              .build();
            assertThrows(MavenDownloadingException.class, () ->
              new MavenPomDownloader(emptyMap(), ctx).downloadMetadata(new GroupArtifact("does.definitely.not", "exist"), null, List.of(repository)));
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/6739")
        @Test
        void deriveMetaDataFromHtmlWithTitleAttributes() throws Exception {
            try (var server = new MockWebServer()) {
                server.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        if (request.getPath() != null && request.getPath().endsWith("maven-metadata.xml")) {
                            return new MockResponse().setResponseCode(404);
                        }
                        return new MockResponse().setResponseCode(200).setBody(
                          """
                            <html><body>
                            <a href="../" title="../">../</a>
                            <a href="1.0.0/" title="1.0.0/">1.0.0/</a>
                            <a href="1.1.0/" title="1.1.0/">1.1.0/</a>
                            <a href="2.0.0/" title="2.0.0/">2.0.0/</a>
                            </body></html>
                            """
                        );
                    }
                });
                server.start();

                MavenRepository repository = MavenRepository.builder()
                  .id("html-with-title")
                  .uri("http://%s:%d/".formatted(server.getHostName(), server.getPort()))
                  .knownToExist(true)
                  .deriveMetadataIfMissing(true)
                  .build();
                MavenMetadata metaData = new MavenPomDownloader(emptyMap(), ctx)
                  .downloadMetadata(new GroupArtifact("fred", "fred"), null, List.of(repository));
                assertThat(metaData.getVersioning().getVersions()).containsExactlyInAnyOrder("1.0.0", "1.1.0", "2.0.0");
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        void mergeMetadata() throws Exception {
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

            var merged = new MavenPomDownloader(emptyMap(), ctx).mergeMetadata(m1, m2);

            assertThat(merged.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20220927.033510");
            assertThat(merged.getVersioning().getSnapshot().getBuildNumber()).isEqualTo("223");
            assertThat(merged.getVersioning().getVersions()).hasSize(4).contains("2.3.2", "2.3.3", "2.4.1", "2.4.2");
            assertThat(merged.getVersioning().getSnapshotVersions()).hasSize(2);
            assertThat(merged.getVersioning().getSnapshotVersions().getFirst().getExtension()).isNotNull();
            assertThat(merged.getVersioning().getSnapshotVersions().getFirst().getValue()).isNotNull();
            assertThat(merged.getVersioning().getSnapshotVersions().getFirst().getUpdated()).isNotNull();
        }

        @Test
        void skipsLocalInvalidArtifactsMissingJar(@TempDir Path localRepository) throws Exception {
            Path localArtifact = localRepository.resolve("com/bad/bad-artifact");
            assertThat(localArtifact.toFile().mkdirs()).isTrue();
            Files.createDirectories(localArtifact.resolve("1"));

            Path localPom = localRepository.resolve("com/bad/bad-artifact/1/bad-artifact-1.pom");
            Files.writeString(localPom,
              //language=xml
              """
                <project>
                  <groupId>com.bad</groupId>
                  <artifactId>bad-artifact</artifactId>
                  <version>1</version>
                </project>
                """
            );

            MavenRepository mavenLocal = MavenRepository.builder()
              .id("local")
              .uri(localRepository.toUri().toString())
              .snapshots(false)
              .knownToExist(true)
              .build();

            // Does not return invalid dependency.
            assertThrows(MavenDownloadingException.class, () ->
              new MavenPomDownloader(emptyMap(), ctx)
                .download(new GroupArtifactVersion("com.bad", "bad-artifact", "1"), null, null, List.of(mavenLocal)));
        }

        @Test
        void skipsLocalInvalidArtifactsEmptyJar(@TempDir Path localRepository) throws Exception {
            Path localArtifact = localRepository.resolve("com/bad/bad-artifact");
            assertThat(localArtifact.toFile().mkdirs()).isTrue();
            Files.createDirectories(localArtifact.resolve("1"));

            Path localPom = localRepository.resolve("com/bad/bad-artifact/1/bad-artifact-1.pom");
            Files.writeString(localPom,
              //language=xml
              """
                <project>
                  <groupId>com.bad</groupId>
                  <artifactId>bad-artifact</artifactId>
                  <version>1</version>
                </project>
                """
            );
            Path localJar = localRepository.resolve("com/bad/bad-artifact/1/bad-artifact-1.jar");
            Files.writeString(localJar, "");

            MavenRepository mavenLocal = MavenRepository.builder()
              .id("local")
              .uri(localRepository.toUri().toString())
              .snapshots(false)
              .knownToExist(true)
              .build();

            // Does not return invalid dependency.
            assertThrows(MavenDownloadingException.class, () ->
              new MavenPomDownloader(emptyMap(), ctx)
                .download(new GroupArtifactVersion("com.bad", "bad-artifact", "1"), null, null, List.of(mavenLocal)));
        }

        @Test
        void dontAllowPomDownloadFailureWithoutJar(@TempDir Path localRepository) {
            MavenRepository mavenLocal = MavenRepository.builder()
              .id("local")
              .uri(localRepository.toUri().toString())
              .snapshots(false)
              .knownToExist(true)
              .build();

            // Do not return invalid dependency
            assertThrows(MavenDownloadingException.class, () -> new MavenPomDownloader(emptyMap(), ctx)
              .download(new GroupArtifactVersion("com.bad", "bad-artifact", "1"), null, null, List.of(mavenLocal)));
        }

        @Test
        void allowPomDownloadFailureWithJar(@TempDir Path localRepository) throws Exception {
            MavenRepository mavenLocal = MavenRepository.builder()
              .id("local")
              .uri(localRepository.toUri().toString())
              .snapshots(false)
              .knownToExist(true)
              .build();

            // Create a valid jar
            Path localJar = localRepository.resolve("com/some/some-artifact/1/some-artifact-1.jar");
            assertThat(localJar.getParent().toFile().mkdirs()).isTrue();
            Files.writeString(localJar, "some content not to be empty");

            // Do not throw exception since we have a jar
            var result = new MavenPomDownloader(emptyMap(), ctx)
              .download(new GroupArtifactVersion("com.some", "some-artifact", "1"), null, null, List.of(mavenLocal));
            assertThat(result.getGav().getGroupId()).isEqualTo("com.some");
            assertThat(result.getGav().getArtifactId()).isEqualTo("some-artifact");
            assertThat(result.getGav().getVersion()).isEqualTo("1");
        }

        @Test
        void doNotRenameRepoForCustomMavenLocal(@TempDir Path tempDir) throws Exception {
            GroupArtifactVersion gav = createArtifact(tempDir);
            MavenExecutionContextView.view(ctx).setLocalRepository(MavenRepository.MAVEN_LOCAL_DEFAULT.withUri(tempDir.toUri().toString()));
            var downloader = new MavenPomDownloader(emptyMap(), ctx);

            var result = downloader.download(gav, null, null, List.of());
            assertThat(result.getRepository()).isNotNull();
            assertThat(result.getRepository().getUri()).startsWith(tempDir.toUri().toString());
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/4080")
        @Test
        void connectTimeout() {
            var downloader = new MavenPomDownloader(ctx);
            var gav = new GroupArtifactVersion("org.openrewrite", "rewrite-core", "7.0.0");
            var repos = singletonList(MavenRepository.builder()
              .id("non-routable").uri("http://10.0.0.0/maven").knownToExist(true).build());

            assertThatThrownBy(() -> downloader.download(gav, null, null, repos))
              .isInstanceOf(MavenDownloadingException.class)
              .hasMessageContaining("rewrite-core")
              .hasMessageContaining("10.0.0.0");
        }

        private static GroupArtifactVersion createArtifact(Path repository) throws IOException {
            Path target = repository.resolve(Path.of("org", "openrewrite", "rewrite", "1.0.0"));
            Path pom = target.resolve("rewrite-1.0.0.pom");
            Path jar = target.resolve("rewrite-1.0.0.jar");
            Files.createDirectories(target);
            Files.createFile(pom);
            Files.createFile(jar);

            Files.write(pom,
              //language=xml
              """
                <project>
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite</artifactId>
                    <version>1.0.0</version>
                </project>
                """.getBytes());
            Files.write(jar, "I'm a jar".getBytes()); // empty jars get ignored
            return new GroupArtifactVersion("org.openrewrite", "rewrite", "1.0.0");
        }

        @Test
        void shouldNotThrowExceptionForModulesInModulesWithRightProperty() {
            var gav = new GroupArtifactVersion("test", "test2", "${test}");

            Path testTest2PomXml = Path.of("test/test2/pom.xml");
            Pom pom = Pom.builder()
              .sourcePath(testTest2PomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .parent(new Parent(new GroupArtifactVersion("test", "test", "${test}"), "../pom.xml"))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "test2", "7.0.0", null))
              .build();

            ResolvedPom resolvedPom = ResolvedPom.builder()
              .requested(pom)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .repositories(singletonList(MAVEN_CENTRAL))
              .build();

            Path testPomXml = Path.of("test/pom.xml");
            Pom pom2 = Pom.builder()
              .sourcePath(testPomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .parent(new Parent(new GroupArtifactVersion("test", "root-test", "${test}"), "../pom.xml"))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "test", "7.0.0", null))
              .build();


            Path rootPomXml = Path.of("pom.xml");
            Pom parentPom = Pom.builder()
              .sourcePath(rootPomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("test", "7.0.0"))
              .parent(null)
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "root-test", "7.0.0", null))
              .build();

            Map<Path, Pom> pomsByPath = new HashMap<>();
            pomsByPath.put(rootPomXml, parentPom);
            pomsByPath.put(testTest2PomXml, pom);
            pomsByPath.put(testPomXml, pom2);

            String httpUrl = "http://%s.com".formatted(UUID.randomUUID());
            var nonexistentRepo = new MavenRepository("repo", httpUrl, null, null, false, null, null, null, null);

            var downloader = new MavenPomDownloader(pomsByPath, ctx);

            assertDoesNotThrow(() -> downloader.download(gav, Objects.requireNonNull(pom.getParent()).getRelativePath(), resolvedPom, singletonList(nonexistentRepo)));
        }

        @Test
        void shouldThrowExceptionForModulesInModulesWithNoRightProperty() {
            var gav = new GroupArtifactVersion("test", "test2", "${test}");

            Path testTest2PomXml = Path.of("test/test2/pom.xml");
            Pom pom = Pom.builder()
              .sourcePath(testTest2PomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .parent(new Parent(new GroupArtifactVersion("test", "test", "${test}"), "../pom.xml"))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "test2", "7.0.0", null))
              .build();

            ResolvedPom resolvedPom = ResolvedPom.builder()
              .requested(pom)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .repositories(singletonList(MAVEN_CENTRAL))
              .build();

            Path testPomXml = Path.of("test/pom.xml");
            Pom pom2 = Pom.builder()
              .sourcePath(testPomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .parent(new Parent(new GroupArtifactVersion("test", "root-test", "${test}"), "../pom.xml"))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "test", "7.0.0", null))
              .build();


            Path rootPomXml = Path.of("pom.xml");
            Pom parentPom = Pom.builder()
              .sourcePath(rootPomXml)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("tt", "7.0.0"))
              .parent(null)
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "test", "root-test", "7.0.0", null))
              .build();

            Map<Path, Pom> pomsByPath = new HashMap<>();
            pomsByPath.put(rootPomXml, parentPom);
            pomsByPath.put(testTest2PomXml, pom);
            pomsByPath.put(testPomXml, pom2);

            String httpUrl = "http://%s.com".formatted(UUID.randomUUID());
            var nonexistentRepo = new MavenRepository("repo", httpUrl, null, null, false, null, null, null, null);

            var downloader = new MavenPomDownloader(pomsByPath, ctx);

            assertThrows(MavenDownloadingException.class, () -> downloader.download(gav, Objects.requireNonNull(pom.getParent()).getRelativePath(), resolvedPom, singletonList(nonexistentRepo)));
        }

        @Test
        void canResolveDifferentVersionOfProjectPom() {
            MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));
            var gav = new GroupArtifactVersion("org.springframework.boot", "spring-boot-starter-parent", "3.0.0");

            Path pomPath = Path.of("pom.xml");
            Pom pom = Pom.builder()
              .sourcePath(pomPath)
              .repository(MAVEN_CENTRAL)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .parent(new Parent(new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", "2.7.0"), null))
              .gav(new ResolvedGroupArtifactVersion(
                "${REPO_URL}", "org.springframework.boot", "spring-boot-starter-parent", "2.7.0", null))
              .build();

            ResolvedPom resolvedPom = ResolvedPom.builder()
              .requested(pom)
              .properties(singletonMap("REPO_URL", MAVEN_CENTRAL.getUri()))
              .repositories(singletonList(MAVEN_CENTRAL))
              .build();

            Map<Path, Pom> pomsByPath = new HashMap<>();
            pomsByPath.put(pomPath, pom);

            String httpUrl = "http://%s.com".formatted(UUID.randomUUID());
            var nonexistentRepo = new MavenRepository("repo", httpUrl, null, null, false, null, null, null, null);

            var downloader = new MavenPomDownloader(pomsByPath, ctx);

            assertDoesNotThrow(() -> downloader.download(gav, Objects.requireNonNull(pom.getParent()).getRelativePath(), resolvedPom, singletonList(nonexistentRepo)));
        }

        @Issue("https://github.com/moderneinc/customer-requests/issues/1950")
        @Test
        void emptyRelativePathSkipsLocalParentLookup() {
            // Parent POM at pom.xml with a version that does NOT match the placeholder
            // in the child's parent reference. This means the parent can only be found
            // via relativePath-based local lookup (step 3 in download()), not by GAV match.
            Path rootPomXml = Path.of("pom.xml");
            Pom parentPom = Pom.builder()
              .sourcePath(rootPomXml)
              .repository(MAVEN_CENTRAL)
              .parent(null)
              .gav(new ResolvedGroupArtifactVersion(
                MAVEN_CENTRAL.getUri(), "test.notreal", "parent", "1.0.0", null))
              .build();

            // Child requests parent with a placeholder version. The placeholder can't
            // be resolved from the parent's own properties, so GAV-based lookup (steps
            // 1 and 2 in download()) won't find the parent. Only step 3 (relativePath-
            // based lookup) can find it, because it relaxes the version check when the
            // requested version contains "${".
            var requestedGav = new GroupArtifactVersion("test.notreal", "parent", "${revision}");
            Path childPomXml = Path.of("child/pom.xml");
            Pom childPom = Pom.builder()
              .sourcePath(childPomXml)
              .repository(MAVEN_CENTRAL)
              .parent(new Parent(requestedGav, ""))
              .gav(new ResolvedGroupArtifactVersion(
                MAVEN_CENTRAL.getUri(), "test.notreal", "child", "1.0.0", null))
              .build();

            ResolvedPom resolvedPom = ResolvedPom.builder()
              .requested(childPom)
              .repositories(singletonList(MAVEN_CENTRAL))
              .build();

            Map<Path, Pom> pomsByPath = new HashMap<>();
            pomsByPath.put(rootPomXml, parentPom);
            pomsByPath.put(childPomXml, childPom);

            // Disable implicit Maven Central and local repo to isolate the test
            var mavenCtx = MavenExecutionContextView.view(ctx);
            mavenCtx.setAddCentralRepository(false);
            mavenCtx.setAddLocalRepository(false);

            String httpUrl = "http://%s.com".formatted(UUID.randomUUID());
            var nonexistentRepo = new MavenRepository("repo", httpUrl, null, null, false, null, null, null, null);

            var downloader = new MavenPomDownloader(pomsByPath, ctx);

            // With null relativePath (omitted <relativePath>), step 3 resolves
            // ../pom.xml and finds the parent because the ${revision} placeholder
            // relaxes the version check.
            assertDoesNotThrow(() -> downloader.download(requestedGav, null, resolvedPom, singletonList(nonexistentRepo)));

            // With empty relativePath (<relativePath/>), step 3 is skipped entirely.
            // Since the parent can't be found by GAV match either, the version still
            // contains an unresolved placeholder, so download throws MavenDownloadingException
            // instead of URISyntaxException from URI.create().
            assertThrows(MavenDownloadingException.class,
              () -> downloader.download(requestedGav, "", resolvedPom, singletonList(nonexistentRepo)));
        }

    }

    @Nested
    class WithOkHttpClientAndSelfSignedTLS {
        private final SSLSocketFactory sslSocketFactory;
        private final ExecutionContext ctx;

        WithOkHttpClientAndSelfSignedTLS() throws UnknownHostException {
            String localhost = InetAddress.getByName("localhost").getCanonicalHostName();
            HeldCertificate localhostCertificate = new HeldCertificate.Builder()
              .addSubjectAlternativeName(localhost)
              .build();
            sslSocketFactory = new HandshakeCertificates.Builder()
              .heldCertificate(localhostCertificate)
              .build().sslSocketFactory();

            HandshakeCertificates clientCertificates = new HandshakeCertificates.Builder()
              .addTrustedCertificate(localhostCertificate.certificate())
              .build();
            OkHttpClient client = new OkHttpClient.Builder()
              .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
              .connectTimeout(Duration.ofMillis(250))
              .readTimeout(Duration.ofMillis(250))
              .build();
            ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
              .setHttpSender(new OkHttpSender(client));
        }

        private MockWebServer getMockServer() {
            var mockWebServer = new MockWebServer();
            mockWebServer.useHttps(sslSocketFactory, false);
            return mockWebServer;
        }

        private void mockServer(Integer responseCode, Consumer<MockWebServer> block) {
            try (MockWebServer mockRepo = getMockServer()) {
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

        @Test
        void useHttpsWhenAvailable() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            mockServer(200, mockRepo -> {
                var normalizedRepository = downloader.normalizeRepository(
                  MavenRepository.builder()
                    .id("id")
                    .uri("http://%s:%d/maven/".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                    .build(),
                  MavenExecutionContextView.view(ctx),
                  null);

                assertThat(normalizedRepository).isEqualTo(
                  MavenRepository.builder()
                    .id("id")
                    .uri("https://%s:%d/maven/".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                    .build());
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {500, 400})
        void normalizeAcceptErrorStatuses(Integer status) {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            mockServer(status, mockRepo -> {
                var originalRepo = MavenRepository.builder()
                  .id("id")
                  .uri("https://%s:%d/maven/".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .build();
                var normalizedRepo = downloader.normalizeRepository(originalRepo, MavenExecutionContextView.view(ctx), null);
                assertThat(normalizedRepo).isEqualTo(originalRepo);
            });
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
                    .hasMessageContaining("https://%s:%d/maven".formatted(repo1.getHostName(), repo1.getPort()))
                    .hasMessageContaining("https://%s:%d/maven".formatted(repo2.getHostName(), repo2.getPort()));
              })
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/3152")
        @Test
        void useSnapshotTimestampVersion() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var gav = new GroupArtifactVersion("fred", "fred", "2020.0.2-20210127.131051-2");
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        assert recordedRequest.getPath() != null;
                        return !recordedRequest.getPath().endsWith("fred/fred/2020.0.2-SNAPSHOT/fred-2020.0.2-20210127.131051-2.pom") ?
                          new MockResponse().setResponseCode(404).setBody("") :
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
        void usesAnonymousRequestIfRepositoryRejectsCredentials() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
            try (MockWebServer mockRepo = getMockServer()) {
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
        void doesNotSendCredentialsWhenRepositoryServesAnonymously() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
            try (MockWebServer mockRepo = getMockServer()) {
                List<@Nullable String> authorizationHeaders = synchronizedList(new ArrayList<>());
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        authorizationHeaders.add(recordedRequest.getHeaders().get("Authorization"));
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <project>
                                <groupId>fred</groupId>
                                <artifactId>fred</artifactId>
                                <version>1.0.0</version>
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

                // Mirror Apache Maven: a repository that serves anonymously must never be sent credentials
                assertThat(authorizationHeaders).containsOnlyNulls();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void authenticatesPreemptivelyAfterCredentialsRequired() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            try (MockWebServer mockRepo = getMockServer()) {
                List<@Nullable String> getRequestAuthHeaders = synchronizedList(new ArrayList<>());
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        if ("GET".equalsIgnoreCase(recordedRequest.getMethod())) {
                            getRequestAuthHeaders.add(recordedRequest.getHeaders().get("Authorization"));
                        }
                        if (recordedRequest.getHeaders().get("Authorization") == null) {
                            return new MockResponse().setResponseCode(401).setBody("");
                        }
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <project>
                                <groupId>fred</groupId>
                                <artifactId>fred</artifactId>
                                <version>1.0.0</version>
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

                assertDoesNotThrow(() -> downloader.download(new GroupArtifactVersion("fred", "fred", "1.0.0"), null, null, repositories));
                assertDoesNotThrow(() -> downloader.download(new GroupArtifactVersion("fred", "other", "1.0.0"), null, null, repositories));

                // Only the first body GET probes anonymously; once the host is known to require credentials, later
                // GETs authenticate preemptively instead of paying another anonymous 401 round-trip.
                assertThat(getRequestAuthHeaders.stream().filter(Objects::isNull).count()).isEqualTo(1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void usesAuthenticationIfRepositoryHasCredentials() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        var response = new MockResponse();
                        if (recordedRequest.getHeaders().get("Authorization") != null) {
                            response.setResponseCode(200);
                            if (!"HEAD".equalsIgnoreCase(recordedRequest.getMethod())) {
                                response.setBody(
                                  //language=xml
                                  """
                                    <project>
                                        <groupId>org.springframework.cloud</groupId>
                                        <artifactId>spring-cloud-dataflow-build</artifactId>
                                        <version>2.10.0-SNAPSHOT</version>
                                    </project>
                                    """);
                            }
                        } else {
                            response.setResponseCode(401);
                        }
                        return response;
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

        @DisplayName("When username or password are environment properties that cannot be resolved, they should not be used")
        @Issue("https://github.com/openrewrite/rewrite/issues/3142")
        @Test
        void doesNotUseAuthenticationIfCredentialsCannotBeResolved() {
            var downloader = new MavenPomDownloader(emptyMap(), ctx);
            var gav = new GroupArtifactVersion("fred", "fred", "1.0.0");
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        var response = new MockResponse();
                        if (recordedRequest.getHeaders().get("Authorization") != null) {
                            response.setResponseCode(401);
                        } else if (recordedRequest.getMethod() == null || !"HEAD".equalsIgnoreCase(recordedRequest.getMethod())) {
                            response.setBody(
                              //language=xml
                              """
                                <project>
                                    <groupId>org.springframework.cloud</groupId>
                                    <artifactId>spring-cloud-dataflow-build</artifactId>
                                    <version>2.10.0-SNAPSHOT</version>
                                </project>
                                """);
                        }
                        return response;
                    }
                });
                mockRepo.start();
                var repositories = List.of(MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .username("${env.ARTIFACTORY_USERNAME}")
                  .password("${env.ARTIFACTORY_USERNAME}")
                  .build());

                assertDoesNotThrow(() -> downloader.download(gav, null, null, repositories));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @DisplayName("Throw exception if there is no pom and no jar for the artifact")
        @Issue("https://github.com/openrewrite/rewrite/issues/4687")
        @Test
        void pomNotFoundWithNoJarShouldThrow() throws Exception {
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        assert recordedRequest.getPath() != null;
                        return new MockResponse().setResponseCode(404).setBody("");
                    }
                });
                mockRepo.start();
                var repositories = List.of(MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .username("user")
                  .password("pass")
                  .build());

                var downloader = new MavenPomDownloader(emptyMap(), ctx);
                var gav = new GroupArtifactVersion("fred", "fred", "1");
                assertThrows(MavenDownloadingException.class, () -> downloader.download(gav, null, null, repositories));
            }
        }

        @DisplayName("Don't throw exception if there is no pom and but there is a jar for the artifact")
        @Issue("https://github.com/openrewrite/rewrite/issues/4687")
        @Test
        void pomNotFoundWithJarFoundShouldNotThrow() throws Exception {
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        assert recordedRequest.getPath() != null;
                        if (recordedRequest.getPath().endsWith("fred/fred/1/fred-1.pom")) {
                            return new MockResponse().setResponseCode(404).setBody("");
                        }
                        return new MockResponse().setResponseCode(200).setBody("some bytes so the jar isn't empty");
                    }
                });
                mockRepo.start();
                var repositories = List.of(MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .username("user")
                  .password("pass")
                  .build());

                var gav = new GroupArtifactVersion("fred", "fred", "1");
                var downloader = new MavenPomDownloader(emptyMap(), ctx);
                Pom downloaded = downloader.download(gav, null, null, repositories);
                assertThat(downloaded.getGav().getGroupId()).isEqualTo("fred");
                assertThat(downloaded.getGav().getArtifactId()).isEqualTo("fred");
                assertThat(downloaded.getGav().getVersion()).isEqualTo("1");
            }
        }

        @DisplayName("Clearly identify which pom failed to parse")
        @Issue("https://github.com/openrewrite/rewrite/issues/5558")
        @Test
        void clearlyIdentifyWhichPomFailedToParse() throws Exception {
            try (MockWebServer mockRepo = getMockServer()) {
                mockRepo.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest recordedRequest) {
                        assert recordedRequest.getPath() != null;
                        if (recordedRequest.getPath().endsWith("fred/fred/1/fred-1.pom")) {
                            // Deliberately malformed pom file, which Maven tolerates, but where jackson-databind fails
                            return new MockResponse().setResponseCode(200).setBody(
                              //language=xml
                              """

                                <?xml version="1.0" encoding="UTF-8"?>
                                <project>
                                    <modelVersion>4.0.0</modelVersion>

                                    <groupId>com.mycompany.app</groupId>
                                    <artifactId>my-app</artifactId>
                                    <version>1</version>
                                </project>
                                """);
                        }
                        return new MockResponse().setResponseCode(200).setBody("some bytes so the jar isn't empty");
                    }
                });
                mockRepo.start();
                var repositories = List.of(MavenRepository.builder()
                  .id("id")
                  .uri("http://%s:%d/maven".formatted(mockRepo.getHostName(), mockRepo.getPort()))
                  .username("user")
                  .password("pass")
                  .build());

                var gav = new GroupArtifactVersion("fred", "fred", "1");
                var downloader = new MavenPomDownloader(emptyMap(), ctx);
                assertThatThrownBy(() -> downloader.download(gav, null, null, repositories))
                  .isInstanceOf(MavenDownloadingException.class)
                  .hasMessageContaining("Unable to download POM: fred:fred:1")
                  .hasMessageContaining("Failed to parse pom")
                  .hasMessageContaining("Illegal processing instruction target (\"xml\")");
            }
        }
    }

    @Test
    void resolveDependencies() throws Exception {
        var ctx = new InMemoryExecutionContext();
        MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));
        var doc = (Xml.Document) MavenParser.builder().build().parse(ctx, """
          <project>
              <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.2.0</version>
                  <relativePath/>
              </parent>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <name>demo</name>
              <dependencies>
                  <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                  </dependency>
              </dependencies>
          </project>
          """).toList().getFirst();
        MavenResolutionResult resolutionResult = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
          .resolveDependencies(new MavenPomDownloader(emptyMap(), ctx, null, null), ctx);
        List<ResolvedDependency> deps = resolutionResult.getDependencies().get(Scope.Compile);
        assertThat(deps).hasSize(34);
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6464")
    @Test
    void emptyClassifierPropertyInIntermediatePom() throws Exception {
        // `azure-spring-data-cosmos` brings in `azure-core-http-netty`, which uses property `<boring-ssl-classifier/>`
        // https://repo1.maven.org/maven2/com/azure/azure-spring-data-cosmos/3.45.0/azure-spring-data-cosmos-3.45.0.pom
        // https://repo1.maven.org/maven2/com/azure/azure-core-http-netty/1.16.2/azure-core-http-netty-1.16.2.pom
        var ctx = new InMemoryExecutionContext();
        MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));
        var doc = (Xml.Document) MavenParser.builder().build().parse(ctx, """
          <project>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>1.0.0</version>
              <properties>
                  <boring-ssl-classifier>something-else</boring-ssl-classifier>
              </properties>
              <dependencies>
                  <dependency>
                      <groupId>com.azure</groupId>
                      <artifactId>azure-spring-data-cosmos</artifactId>
                      <version>3.45.0</version>
                  </dependency>
              </dependencies>
          </project>
          """).toList().getFirst();
        MavenResolutionResult resolutionResult = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
          .resolveDependencies(new MavenPomDownloader(emptyMap(), ctx, null, null), ctx);
        List<ResolvedDependency> deps = resolutionResult.getDependencies().get(Scope.Compile);
        assertThat(deps)
          .filteredOn(rd -> "io.netty".equals(rd.getGroupId()))
          .filteredOn(rd -> "netty-tcnative-boringssl-static".equals(rd.getArtifactId()))
          .isNotEmpty()
          .extracting(ResolvedDependency::getClassifier)
          .doesNotContain("${boring-ssl-classifier}")
          .doesNotContain("something-else")
          .contains("")
          .anyMatch(c -> !"".equals(c));
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/7685")
    @Test
    void doesNotThrowONMissingModuleWhenNot404() throws Exception {
        // Mimics an Artifactory virtual repository accessed anonymously: it answers 401 (not 404)
        // for artifacts it will not serve, and 200 for the ones it does. A local mock server keeps
        // the test hermetic instead of depending on a live repository's (drifting) contents.
        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath() == null ? "" : request.getPath();
                    // Existing group:artifact metadata
                    if (path.endsWith("/org/springframework/integration/spring-integration-bom/maven-metadata.xml")) {
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <metadata>
                                <groupId>org.springframework.integration</groupId>
                                <artifactId>spring-integration-bom</artifactId>
                                <versioning>
                                    <latest>5.5.0</latest>
                                    <release>5.5.0</release>
                                    <versions>
                                        <version>5.5.0</version>
                                    </versions>
                                </versioning>
                            </metadata>
                            """);
                    }
                    // Existing snapshot-version metadata
                    if (path.endsWith("/com/fasterxml/jackson/jackson-base/2.19.3-SNAPSHOT/maven-metadata.xml")) {
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <metadata modelVersion="1.1.0">
                                <groupId>com.fasterxml.jackson</groupId>
                                <artifactId>jackson-base</artifactId>
                                <version>2.19.3-SNAPSHOT</version>
                                <versioning>
                                    <snapshot>
                                        <timestamp>20240101.000000</timestamp>
                                        <buildNumber>1</buildNumber>
                                    </snapshot>
                                    <lastUpdated>20240101000000</lastUpdated>
                                </versioning>
                            </metadata>
                            """);
                    }
                    // Existing release POM, declaring Gradle module metadata so the downloader fetches the `.module` side-car
                    if (path.endsWith("/org/springframework/integration/spring-integration-bom/5.5.0/spring-integration-bom-5.5.0.pom")) {
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <project>
                                <!-- do_not_remove: published-with-gradle-metadata -->
                                <modelVersion>4.0.0</modelVersion>
                                <groupId>org.springframework.integration</groupId>
                                <artifactId>spring-integration-bom</artifactId>
                                <version>5.5.0</version>
                                <packaging>pom</packaging>
                            </project>
                            """);
                    }
                    // Everything else - missing artifacts and the Gradle `.module` side-car - answers 401, never 404.
                    return new MockResponse().setResponseCode(401);
                }
            });
            mockRepo.start();

            MavenPomDownloader downloader = new MavenPomDownloader(new InMemoryExecutionContext());
            List<MavenRepository> repositories = singletonList(MavenRepository.builder()
              .id("cache-3")
              .uri(mockRepo.url("/").toString())
              .knownToExist(true)
              .build());
            GroupArtifact unexisting = new GroupArtifact("org.springframework.integration", "fail");
            GroupArtifact existing = new GroupArtifact("org.springframework.integration", "spring-integration-bom");

            // Missing module: the repo answers 401 (not 404), but no metadata is retrievable -> still throws
            assertThrows(MavenDownloadingException.class, () -> downloader.downloadMetadata(unexisting, null, repositories));
            // Existing group:artifact metadata (200) -> does not throw
            assertDoesNotThrow(() -> downloader.downloadMetadata(existing, null, repositories));
            // Missing release-version metadata (401) -> throws
            assertThrows(MavenDownloadingException.class, () -> downloader.downloadMetadata(new GroupArtifactVersion("com.fasterxml.jackson", "jackson-base", "2.19.3"), null, repositories));
            // Existing snapshot-version metadata (200) -> does not throw
            assertDoesNotThrow(() -> downloader.downloadMetadata(new GroupArtifactVersion("com.fasterxml.jackson", "jackson-base", "2.19.3-SNAPSHOT"), null, repositories));
            // Missing POM version (401) -> throws
            assertThrows(MavenDownloadingException.class, () -> downloader.download(new GroupArtifactVersion(existing.getGroupId(), existing.getArtifactId(), "5.5.-1"), null, null, repositories));
            // Existing POM whose Gradle `.module` side-car returns 401 (not 404) -> does not throw (PR #7685)
            assertDoesNotThrow(() -> downloader.download(new GroupArtifactVersion(existing.getGroupId(), existing.getArtifactId(), "5.5.0"), null, null, repositories));
        }
    }

    @Nested
    class ConditionalMetadataRequests {
        private final ExecutionContext ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)));

        @Language("xml")
        private static final String METADATA_V1 = """
          <metadata>
            <groupId>org.openrewrite.test</groupId>
            <artifactId>foo</artifactId>
            <versioning>
              <latest>1.0</latest>
              <release>1.0</release>
              <versions>
                <version>1.0</version>
              </versions>
            </versioning>
          </metadata>
          """;

        @Language("xml")
        private static final String METADATA_V2 = """
          <metadata>
            <groupId>org.openrewrite.test</groupId>
            <artifactId>foo</artifactId>
            <versioning>
              <latest>2.0</latest>
              <release>2.0</release>
              <versions>
                <version>1.0</version>
                <version>2.0</version>
              </versions>
            </versioning>
          </metadata>
          """;

        private static final GroupArtifact GA = new GroupArtifact("org.openrewrite.test", "foo");

        private MavenPomDownloader downloader(MavenPomCache cache) {
            ExecutionContext mavenCtx = MavenExecutionContextView.view(ctx)
              .setAddCentralRepository(false)
              .setAddLocalRepository(false)
              .setPomCache(cache);
            return new MavenPomDownloader(emptyMap(), mavenCtx);
        }

        private static MavenRepository repo(MockWebServer server) {
            return MavenRepository.builder().id("test").uri(server.url("/").toString()).build();
        }

        /**
         * A dispatcher that records every {@code maven-metadata.xml} request into {@code sink} and
         * delegates its response to {@code onMetadata}, while answering repository-normalization probes
         * (OPTIONS/HEAD against the base URL) with a plain 200.
         */
        private static Dispatcher metadataDispatcher(List<RecordedRequest> sink, Function<RecordedRequest, MockResponse> onMetadata) {
            return new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath();
                    if (path != null && path.endsWith("maven-metadata.xml")) {
                        sink.add(request);
                        return onMetadata.apply(request);
                    }
                    return new MockResponse().setResponseCode(200);
                }
            };
        }

        @Test
        void reusesCachedValueOn304() throws Exception {
            String etag = "\"v1\"";
            List<RecordedRequest> metadataRequests = new ArrayList<>();
            try (MockWebServer server = new MockWebServer()) {
                server.setDispatcher(metadataDispatcher(metadataRequests, request ->
                  etag.equals(request.getHeader("If-None-Match")) ?
                    new MockResponse().setResponseCode(304) :
                    new MockResponse().setResponseCode(200).setHeader("ETag", etag).setBody(METADATA_V1)));
                server.start();
                MavenPomDownloader downloader = downloader(new RetainingPomCache());
                MavenRepository repo = repo(server);

                // Cold: unconditional GET -> 200, caches value + ETag validator.
                MavenMetadata first = downloader.downloadMetadata(GA, null, List.of(repo));
                assertThat(first.getVersioning().getVersions()).containsExactly("1.0");

                // Warm-but-expired: getMavenMetadata misses, but the retained validator drives a
                // conditional GET -> 304 -> the cached value is reused without re-parsing a body.
                MavenMetadata second = downloader.downloadMetadata(GA, null, List.of(repo));
                assertThat(second.getVersioning().getVersions()).containsExactly("1.0");

                assertThat(metadataRequests).hasSize(2);
                assertThat(metadataRequests.get(0).getHeader("If-None-Match")).isNull();
                assertThat(metadataRequests.get(1).getHeader("If-None-Match")).isEqualTo(etag);
            }
        }

        @Test
        void downloadsNewMetadataWhenChanged() throws Exception {
            // The origin's current validator and body; flipping these simulates a newly published version.
            String[] currentEtag = {"\"v1\""};
            String[] currentBody = {METADATA_V1};
            List<RecordedRequest> metadataRequests = new ArrayList<>();
            try (MockWebServer server = new MockWebServer()) {
                server.setDispatcher(metadataDispatcher(metadataRequests, request ->
                  currentEtag[0].equals(request.getHeader("If-None-Match")) ?
                    new MockResponse().setResponseCode(304) :
                    new MockResponse().setResponseCode(200).setHeader("ETag", currentEtag[0]).setBody(currentBody[0])));
                server.start();
                MavenPomDownloader downloader = downloader(new RetainingPomCache());
                MavenRepository repo = repo(server);

                // Cold download caches v1 + its ETag.
                assertThat(downloader.downloadMetadata(GA, null, List.of(repo)).getVersioning().getVersions()).containsExactly("1.0");

                // A new version is published: the origin's validator no longer matches, so the conditional
                // GET (If-None-Match: v1) is answered with a fresh 200 carrying the new value and validator.
                currentEtag[0] = "\"v2\"";
                currentBody[0] = METADATA_V2;
                assertThat(downloader.downloadMetadata(GA, null, List.of(repo)).getVersioning().getVersions()).containsExactly("1.0", "2.0");

                // The new validator (v2) was stored, so a subsequent revalidation is a 304 reusing v2.
                assertThat(downloader.downloadMetadata(GA, null, List.of(repo)).getVersioning().getVersions()).containsExactly("1.0", "2.0");

                assertThat(metadataRequests).hasSize(3);
                assertThat(metadataRequests.get(0).getHeader("If-None-Match")).isNull();
                assertThat(metadataRequests.get(1).getHeader("If-None-Match")).isEqualTo("\"v1\"");
                assertThat(metadataRequests.get(2).getHeader("If-None-Match")).isEqualTo("\"v2\"");
            }
        }

        @Test
        void sendsNoConditionalHeadersWhenCacheRetainsNoValidator() throws Exception {
            List<RecordedRequest> metadataRequests = new ArrayList<>();
            try (MockWebServer server = new MockWebServer()) {
                server.setDispatcher(metadataDispatcher(metadataRequests, request ->
                  new MockResponse().setResponseCode(200).setHeader("ETag", "\"v1\"").setBody(METADATA_V1)));
                server.start();

                // A cache that always misses and never retains a validator, so every lookup falls back
                // to a plain, unconditional download.
                MavenPomCache nonRevalidating = new InMemoryMavenPomCache() {
                    @Override
                    public @Nullable MavenMetadataCacheEntry getMavenMetadata(URI repo, GroupArtifactVersion gav) {
                        return null;
                    }
                };
                MavenPomDownloader downloader = downloader(nonRevalidating);
                MavenRepository repo = repo(server);

                downloader.downloadMetadata(GA, null, List.of(repo));
                downloader.downloadMetadata(GA, null, List.of(repo));

                assertThat(metadataRequests).hasSize(2);
                assertThat(metadataRequests).allSatisfy(r -> assertThat(r.getHeader("If-None-Match")).isNull());
            }
        }

        /**
         * A cache that always reports its retained entries as {@linkplain MavenMetadataCacheEntry#isExpired()
         * expired} (simulating an elapsed freshness window), so a cached value is never served directly
         * but its validators still drive a conditional GET.
         */
        static class RetainingPomCache extends InMemoryMavenPomCache {
            private final Map<String, MavenMetadataCacheEntry> retained = new HashMap<>();

            private static String key(URI repo, GroupArtifactVersion gav) {
                return repo + "|" + gav;
            }

            @Override
            public @Nullable MavenMetadataCacheEntry getMavenMetadata(URI repo, GroupArtifactVersion gav) {
                MavenMetadataCacheEntry entry = retained.get(key(repo, gav));
                return entry == null ? null : entry.withExpired(true);
            }

            @Override
            public void putMavenMetadata(URI repo, GroupArtifactVersion gav, MavenMetadataCacheEntry metadata) {
                retained.put(key(repo, gav), metadata);
            }
        }
    }
}
