/*
 * Copyright 2022 the original author or authors.
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

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.maven.http.OkHttpSender;
import org.openrewrite.test.RewriteTest;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;

import static org.openrewrite.maven.Assertions.pomXml;

class AddRepositoryTest implements RewriteTest {

    @DocumentExample
    @Test
    void addSimpleRepo() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void addSimplePluginRepo() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null, AddRepository.Type.PluginRepository)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateExistingRepo() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", "bb", null,
            null, null, null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>qq</name>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>bb</name>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void doNotRemoveRepoName() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>qq</name>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void removeSnapshots() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateSnapshots1() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            false, "whatever", null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>false</enabled>
                      <checksumPolicy>whatever</checksumPolicy>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateSnapshots2() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, "whatever", null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <checksumPolicy>whatever</checksumPolicy>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void noIdMatch1SameSnapshots() {
        rewriteRun(
          spec -> spec.recipe(new AddRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            true, null, null,
            null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo-X</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateToSpringBoot30Snapshot() throws Exception {
        // Serve over TLS: MavenPomDownloader#normalizeRepository probes https first and only falls back to
        // http once that fails, so a plaintext mock costs two doomed handshakes per repository before anything
        // resolves.
        HeldCertificate certificate = new HeldCertificate.Builder()
          .addSubjectAlternativeName(InetAddress.getByName("localhost").getCanonicalHostName())
          .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
          .heldCertificate(certificate)
          .build();
        HandshakeCertificates clientCertificates = new HandshakeCertificates.Builder()
          .addTrustedCertificate(certificate.certificate())
          .build();

        try (MockWebServer mockRepo = new MockWebServer()) {
            mockRepo.useHttps(serverCertificates.sslSocketFactory(), false);
            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath();
                    if (path == null) {
                        return new MockResponse().setResponseCode(404);
                    }
                    if (path.endsWith("/spring-boot-starter-parent/maven-metadata.xml")) {
                        return new MockResponse().setResponseCode(200).setBody(
                          //language=xml
                          """
                            <metadata>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-starter-parent</artifactId>
                              <versioning>
                                <versions>
                                  <version>2.7.3</version>
                                  <version>3.0.0-SNAPSHOT</version>
                                </versions>
                              </versioning>
                            </metadata>
                            """);
                    }
                    if (path.endsWith("/spring-boot-starter-parent-2.7.3.pom")) {
                        return new MockResponse().setResponseCode(200).setBody(parentPom("2.7.3"));
                    }
                    if (path.endsWith("/spring-boot-starter-parent-3.0.0-SNAPSHOT.pom")) {
                        return new MockResponse().setResponseCode(200).setBody(parentPom("3.0.0-SNAPSHOT"));
                    }
                    // No dated-snapshot metadata, so the plain -SNAPSHOT file name is requested
                    return new MockResponse().setResponseCode(404);
                }
            });
            mockRepo.start();

            // Mirroring everything means the repository the recipe adds is served by the mock too, so the
            // test asserts on the URL the recipe writes without depending on that host being reachable.
            MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                    <mirrors>
                        <mirror>
                            <id>mock</id>
                            <mirrorOf>*</mirrorOf>
                            <url>https://%s:%d</url>
                        </mirror>
                    </mirrors>
                </settings>
                """.formatted(mockRepo.getHostName(), mockRepo.getPort())
            ), new InMemoryExecutionContext());

            OkHttpClient client = new OkHttpClient.Builder()
              .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
              .connectTimeout(Duration.ofSeconds(1))
              .readTimeout(Duration.ofSeconds(1))
              .build();

            rewriteRun(
              spec -> spec
                .recipes(
                  new AddRepository("boot-snapshots", "https://repo.spring.io/snapshot", null, null,
                    true, null, null,
                    null, null, null, null),
                  new UpgradeParentVersion(
                    "org.springframework.boot",
                    "spring-boot-starter-parent",
                    "3.0.0-SNAPSHOT",
                    null,
                    null)
                )
                .executionContext(MavenExecutionContextView.view(
                    HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
                      .setHttpSender(new OkHttpSender(client)))
                  .setMavenSettings(settings)),
              pomXml(
                """
                  <project>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.7.3</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                """
                  <project>
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.0.0-SNAPSHOT</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <repositories>
                      <repository>
                        <id>boot-snapshots</id>
                        <url>https://repo.spring.io/snapshot</url>
                        <snapshots>
                          <enabled>true</enabled>
                        </snapshots>
                      </repository>
                    </repositories>
                  </project>
                  """
              )
            );
        }
    }

    @Language("xml")
    private static String parentPom(String version) {
        return """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>%s</version>
            <packaging>pom</packaging>
          </project>
          """.formatted(version);
    }
}
