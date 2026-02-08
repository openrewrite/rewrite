/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryMirror;
import org.openrewrite.xml.SemanticallyEqual;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"HttpUrlsUsage", "ConstantConditions", "OptionalGetWithoutIsPresent"})
@Isolated("Modifies user.home system property")
@Execution(ExecutionMode.SAME_THREAD)
class MavenSettingsTest {

    private final MavenExecutionContextView ctx = MavenExecutionContextView.view(
      new InMemoryExecutionContext((ThrowingConsumer<Throwable>) input -> {
          throw input;
      }));

    final String MASTER_PASS_ENCRYPTED = "{6hp0oENJ604H81U6AqPSAJNapKivabHsuWVHzJoZJJo=}";
    final String ENCRYPTED_PASSWORD = "{2XRZmSPonBYHUeRGefWQTymnQks33CW6U1NHTEtSOH4=}";

    private String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void parse() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://repo.spring.io/milestone</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getRepositories()).hasSize(1);
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1155")
    @Test
    void parseWithEncryption() throws Exception {
        createSettingsSecurityFile();

        MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>%s</password>
                    </server>
                  </servers>
            </settings>
            """.formatted(ENCRYPTED_PASSWORD)
        ), ctx);

        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> "my_login".equals(repo.getUsername()))
          .matches(repo -> "password".equals(repo.getPassword()));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1155")
    @Test
    void parsePlainTextWithEncryption() throws Exception {
        createSettingsSecurityFile();
        String plainTextPassword = "password";
        MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>%s</password>
                    </server>
                  </servers>
            </settings>
            """.formatted(plainTextPassword)
        ), ctx);

        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> "my_login".equals(repo.getUsername()))
          .matches(repo -> repo.getPassword().equals(plainTextPassword));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    void defaultActiveWhenNoOthersAreActive() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <profiles>
                    <profile>
                        <id>default</id>
                        <activation>
                            <activeByDefault>true</activeByDefault>
                        </activation>
                        <repositories>
                            <repository>
                                <id>spring-milestones-default</id>
                                <name>Spring Milestones</name>
                                <url>https://activebydefault.com</url>
                            </repository>
                        </repositories>
                    </profile>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://actviebyactivationlist.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getRepositories().stream().map(MavenRepository::getUri)).containsExactly("https://activebydefault.com");
    }

    @Test
    void idCollisionLastRepositoryWins() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>repo</id>
                                <url>https://firstloses.com</url>
                            </repository>
                            <repository>
                                <id>repo</id>
                                <url>https://secondloses.com</url>
                            </repository>
                            <repository>
                                <id>repo</id>
                                <url>https://lastwins.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getRepositories())
          .as("When multiple repositories have the same id in a maven settings file the last one wins. In a pom.xml an error would be thrown.")
          .containsExactly(new MavenRepository("repo", "https://lastwins.com", null, null, null, null, null));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    void defaultOnlyActiveIfNoOthersAreActive() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>default</id>
                        <activation>
                            <activeByDefault>true</activeByDefault>
                        </activation>
                        <repositories>
                            <repository>
                                <id>spring-milestones-default</id>
                                <name>Spring Milestones</name>
                                <url>https://activebydefault.com</url>
                            </repository>
                        </repositories>
                    </profile>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://activebyactivationlist.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getActiveProfiles())
          .containsExactly("repo");

        assertThat(ctx.getRepositories().stream().map(MavenRepository::getUri))
          .containsExactly("https://activebyactivationlist.com");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/130")
    @Test
    void mirrorReplacesRepository() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <url>https://externalrepository.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <mirrorOf>*</mirrorOf>
                        <name>repo</name>
                        <url>https://internalartifactrepository.yourorg.com</url>
                        <id>repo</id>
                    </mirror>
                </mirrors>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getRepositories().stream()
          .map(repo -> MavenRepositoryMirror.apply(ctx.getMirrors(), repo))
          .map(MavenRepository::getUri))
          .containsExactly("https://internalartifactrepository.yourorg.com");
    }

    @Test
    void starredMirrorWithExclusion() {
        ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>should-be-mirrored</id>
                                <url>https://externalrepository.com</url>
                            </repository>
                            <repository>
                                <id>should-not-be-mirrored</id>
                                <url>https://externalrepository.com</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
                <mirrors>
                    <mirror>
                        <mirrorOf>*,!should-not-be-mirrored</mirrorOf>
                        <name>repo</name>
                        <url>https://internalartifactrepository.yourorg.com</url>
                        <id>repo</id>
                    </mirror>
                </mirrors>
            </settings>
            """
        ), ctx));

        assertThat(ctx.getRepositories().stream()
          .map(repo -> MavenRepositoryMirror.apply(ctx.getMirrors(), repo)))
          .hasSize(2)
          .haveAtLeastOne(
            new Condition<>(repo -> "https://internalartifactrepository.yourorg.com".equals(repo.getUri()),
              "Repository should-be-mirrored should have had its URL changed to https://internalartifactrepository.yourorg.com"
            )
          ).haveAtLeastOne(
            new Condition<>(repo -> "https://externalrepository.com".equals(repo.getUri()) &&
              "should-not-be-mirrored".equals(repo.getId()),
              "Repository should-not-be-mirrored should have had its URL left unchanged"
            )
          );
    }

    @Test
    void serverCredentials() {
        var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                  <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>my_password</password>
                    </server>
                  </servers>
            </settings>
            """
        ), ctx);

        assertThat(settings.getServers()).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1);
        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> "my_login".equals(repo.getUsername()))
          .matches(repo -> "my_password".equals(repo.getPassword()));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1155")
    @Test
    void serverCredentialsWithEncryption() throws Exception {
        createSettingsSecurityFile();

        MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>%s</password>
                    </server>
                  </servers>
            </settings>
            """.formatted(ENCRYPTED_PASSWORD)
        ), ctx);

        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> "my_login".equals(repo.getUsername()))
          .matches(repo -> "password".equals(repo.getPassword()));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1155")
    @Test
    void serverCredentialsPlainTextWithEncryption() throws Exception {
        createSettingsSecurityFile();
        String plainTextPassword = "password";

        MavenSettings settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <servers>
                    <server>
                      <id>server001</id>
                      <username>my_login</username>
                      <password>%s</password>
                    </server>
                  </servers>
            </settings>
            """.formatted(plainTextPassword)
        ), ctx);

        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> "my_login".equals(repo.getUsername()))
          .matches(repo -> repo.getPassword().equals(plainTextPassword));
    }

    @Test
    void serverTimeouts() {
        // Deliberately supporting the simpler old configuration of a single timeout
        // https://maven.apache.org/guides/mini/guide-http-settings.html#connection-timeouts
        var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                  <servers>
                    <server>
                      <id>server001</id>
                      <configuration>
                        <timeout>40000</timeout>
                      </configuration>
                    </server>
                  </servers>
            </settings>
            """
        ), ctx);

        assertThat(settings.getServers()).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1);
        assertThat(settings.getServers().getServers().getFirst())
          .matches(repo -> "server001".equals(repo.getId()))
          .matches(repo -> repo.getConfiguration().getTimeout().equals(40000L));
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite/issues/1688")
    class LocalRepositoryTest {
        @Test
        void parsesLocalRepositoryPathFromSettingsXml(@TempDir Path localRepoPath) {
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                      <localRepository>%s</localRepository>
                </settings>
                """.formatted(localRepoPath)
            ), ctx));
            assertThat(ctx.getLocalRepository().getUri())
              .startsWith("file://")
              .containsSubsequence(localRepoPath.toUri().toString().split("/"));
        }

        @Test
        void parsesLocalRepositoryUriFromSettingsXml(@TempDir Path localRepoPath) {
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                      <localRepository>%s</localRepository>
                </settings>
                """.formatted(localRepoPath)
            ), ctx));

            assertThat(ctx.getLocalRepository().getUri())
              .startsWith("file://")
              .containsSubsequence(localRepoPath.toUri().toString().split("/"));
        }

        @Test
        void defaultsToTheMavenDefault() {
            ctx.setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    </settings>
                """
            ), ctx));

            assertThat(ctx.getLocalRepository().getUri()).isEqualTo(MavenRepository.MAVEN_LOCAL_DEFAULT.getUri());
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1801")
    @Nested
    class InterpolationTest {
        @Test
        void properties() {
            System.setProperty("rewrite.test.custom.location", "/tmp");
            var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    <servers>
                         <server>
                             <id>private-repo</id>
                             <username>my_username</username>
                             <password>my_pass</password>
                        </server>
                    </servers>
                    <localRepository>${rewrite.test.custom.location}/maven/local/repository/</localRepository>
                    <activeProfiles>
                        <activeProfile>
                            my-profile
                        </activeProfile>
                    </activeProfiles>
                    <profiles>
                        <profile>
                            <id>my-profile</id>
                            <repositories>
                                <repository>
                                    <id>private-repo</id>
                                    <name>Private Repo</name>
                                    <url>https://repo.company.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </settings>
                """
            ), ctx);

            assertThat(settings.getLocalRepository()).isEqualTo("/tmp/maven/local/repository/");
        }

        @Test
        void unresolvedPlaceholdersRemainUnchanged() {
            var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    <servers>
                         <server>
                             <id>private-repo</id>
                             <username>${env.PRIVATE_REPO_USERNAME_ZZ}</username>
                             <password>${env.PRIVATE_REPO_PASSWORD_ZZ}</password>
                        </server>
                    </servers>
                    <localRepository>${custom.location.zz}/maven/local/repository/</localRepository>
                    <activeProfiles>
                        <activeProfile>
                            my-profile
                        </activeProfile>
                    </activeProfiles>
                    <profiles>
                        <profile>
                            <id>my-profile</id>
                            <repositories>
                                <repository>
                                    <id>private-repo</id>
                                    <name>Private Repo</name>
                                    <url>https://repo.company.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </settings>
                """
            ), ctx);

            assertThat(settings.getLocalRepository())
              .isEqualTo("${custom.location.zz}/maven/local/repository/");
            assertThat(settings.getServers().getServers().getFirst().getUsername()).isEqualTo("${env.PRIVATE_REPO_USERNAME_ZZ}");
            assertThat(settings.getServers().getServers().getFirst().getPassword()).isEqualTo("${env.PRIVATE_REPO_PASSWORD_ZZ}");
        }

        @Test
        @Disabled("Depends on methods which are unstable in CI build")
        void env() {
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_USERNAME", "user");
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_PASSWORD", "pass");
            var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    <servers>
                         <server>
                             <id>private-repo</id>
                             <username>${env.REWRITE_TEST_PRIVATE_REPO_USERNAME}</username>
                             <password>${env.REWRITE_TEST_PRIVATE_REPO_PASSWORD}</password>
                        </server>
                    </servers>
                    <localRepository>/tmp/maven/local/repository/</localRepository>
                    <activeProfiles>
                        <activeProfile>
                            my-profile
                        </activeProfile>
                    </activeProfiles>
                    <profiles>
                        <profile>
                            <id>my-profile</id>
                            <repositories>
                                <repository>
                                    <id>private-repo</id>
                                    <name>Private Repo</name>
                                    <url>https://repo.company.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </settings>
                """
            ), ctx);

            assertThat(settings.getServers()).isNotNull();
            assertThat(settings.getServers().getServers()).hasSize(1);
            assertThat(settings.getServers().getServers().getFirst().getUsername()).isEqualTo("user");
            assertThat(settings.getServers().getServers().getFirst().getPassword()).isEqualTo("pass");
        }

        /**
         * Unusable with Java 17 (and therefore in CI builds)
         */
        @SuppressWarnings("UNCHECKED_CAST")
        private static void updateEnvMap(String name, String value) {
            try {
                var env = System.getenv();
                Field field = env.getClass().getDeclaredField("m");
                field.setAccessible(true);
                //noinspection unchecked
                ((Map<String, String>) field.get(env)).put(name, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Nested
    class MergingTest {
        @Language("xml")
        private final String installationSettings = """
          <settings>
              <servers>
                   <server>
                       <id>private-repo</id>
                       <username>user</username>
                       <password>secret</password>
                  </server>
              </servers>
              <localRepository>${user.home}/maven/local/repository/</localRepository>
              <activeProfiles>
                  <activeProfile>
                      my-profile
                  </activeProfile>
              </activeProfiles>
              <profiles>
                  <profile>
                      <id>my-profile</id>
                      <repositories>
                          <repository>
                              <id>private-repo</id>
                              <name>Private Repo</name>
                              <url>https://repo.company.net/maven</url>
                          </repository>
                      </repositories>
                  </profile>
              </profiles>
              <mirrors>
                  <mirror>
                      <id>planetmirror.com</id>
                      <name>PlanetMirror Australia</name>
                      <url>http://downloads.planetmirror.com/pub/maven2</url>
                      <mirrorOf>central</mirrorOf>
                  </mirror>
              </mirrors>
          </settings>
          """;

        @Test
        void concatenatesElementsWithUniqueIds() {
            Path path = Path.of("settings.xml");
            var baseSettings = MavenSettings.parse(Parser.Input.fromString(path, installationSettings), ctx);
            var userSettings = MavenSettings.parse(Parser.Input.fromString(path,
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    <servers>
                         <server>
                             <id>private-repo-2</id>
                             <username>user</username>
                             <password>secret</password>
                        </server>
                    </servers>
                    <activeProfiles>
                        <activeProfile>
                            my-profile-2
                        </activeProfile>
                    </activeProfiles>
                    <profiles>
                        <profile>
                            <id>my-profile-2</id>
                            <repositories>
                                <repository>
                                    <id>private-repo-2</id>
                                    <name>Private Repo</name>
                                    <url>https://repo.company.net/maven</url>
                                    <snapshots>
                                        <enabled>true</enabled>
                                        <updatePolicy>never</updatePolicy>
                                        <checksumPolicy>fail</checksumPolicy>
                                    </snapshots>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                    <mirrors>
                        <mirror>
                            <id>planetmirror.com-2</id>
                            <name>PlanetMirror Australia</name>
                            <url>http://downloads.planetmirror.com/pub/maven2</url>
                            <mirrorOf>central</mirrorOf>
                        </mirror>
                    </mirrors>
                </settings>
                """
            ), ctx);

            var mergedSettings = userSettings.merge(baseSettings);

            assertThat(mergedSettings.getProfiles().getProfiles()).hasSize(2);
            assertThat(mergedSettings.getActiveProfiles().getActiveProfiles()).hasSize(2);
            assertThat(mergedSettings.getMirrors().getMirrors()).hasSize(2);
            assertThat(mergedSettings.getServers().getServers()).hasSize(2);
        }

        @Test
        void mergedOrderingPutsFirstSettingsFirst() {
            MavenSettings baseSettings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                    <profiles>
                        <profile>
                            <id>first-profile</id>
                            <repositories>
                                <repository>
                                    <id>first-repo</id>
                                    <name>Private First Repo</name>
                                    <url>https://repo.company1.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </settings>
                """
            ), ctx);
            MavenSettings userSettings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                    <profiles>
                        <profile>
                            <id>second-profile</id>
                            <repositories>
                                <repository>
                                    <id>second-repo</id>
                                    <name>Private Second Repo</name>
                                    <url>https://repo.company2.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </settings>
                """
            ), ctx);

            MavenSettings mergedSettings = baseSettings.merge(userSettings);
            assertThat(mergedSettings.getProfiles().getProfiles().getFirst().getId()).isEqualTo("first-profile");
            assertThat(mergedSettings.getProfiles().getProfiles().get(1).getId()).isEqualTo("second-profile");
            assertThat(mergedSettings.getProfiles().getProfiles().getFirst().getRepositories().getRepositories().getFirst().getId()).isEqualTo("first-repo");
            assertThat(mergedSettings.getProfiles().getProfiles().get(1).getRepositories().getRepositories().getFirst().getId()).isEqualTo("second-repo");
        }

        @Test
        void replacesElementsWithMatchingIds() {
            var baseSettings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"), installationSettings), ctx);
            var userSettings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                    <servers>
                         <server>
                             <id>private-repo</id>
                             <username>foo</username>
                        </server>
                    </servers>
                    <profiles>
                        <profile>
                            <id>my-profile</id>
                            <repositories>
                                <repository>
                                    <id>private-repo</id>
                                    <name>Private Repo</name>
                                    <url>https://repo.company.net/maven</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                    <mirrors>
                        <mirror>
                            <id>planetmirror.com</id>
                            <name>PlanetMirror Australia</name>
                            <url>http://downloads.planetmirror.com/pub/maven3000</url>
                            <mirrorOf>central</mirrorOf>
                        </mirror>
                    </mirrors>
                </settings>
                """
            ), ctx);

            var mergedSettings = userSettings.merge(baseSettings);

            assertThat(mergedSettings.getProfiles().getProfiles()).hasSize(1);
            assertThat(mergedSettings.getProfiles().getProfiles().getFirst().getRepositories().getRepositories().getFirst().getSnapshots()).isNull();
            assertThat(mergedSettings.getActiveProfiles().getActiveProfiles()).hasSize(1);
            assertThat(mergedSettings.getMirrors().getMirrors()).hasSize(1);

            assertThat(mergedSettings.getMirrors().getMirrors().getFirst().getUrl())
              .isEqualTo("http://downloads.planetmirror.com/pub/maven3000");

            assertThat(mergedSettings.getServers().getServers()).hasSize(1);
            assertThat(mergedSettings.getServers().getServers().getFirst())
              .hasFieldOrPropertyWithValue("username", "foo")
              .hasFieldOrPropertyWithValue("password", null);
        }
    }

    /**
     * See the <a href="https://maven.apache.org/guides/mini/guide-http-settings.html#Taking_Control_of_Your_HTTP_Headers">Maven guide</a>
     * on HTTP headers.
     */
    @Test
    void serverHttpHeaders() {
        var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings>
                <servers>
                    <server>
                        <id>maven-snapshots</id>
                        <configuration>
                            <httpHeaders>
                                <property>
                                    <name>X-JFrog-Art-Api</name>
                                    <value>myApiToken</value>
                                </property>
                            </httpHeaders>
                        </configuration>
                    </server>
                </servers>
                <profiles>
                    <profile>
                        <id>my-profile</id>
                        <repositories>
                            <repository>
                                <id>maven-snapshots</id>
                                <name>Private Repo</name>
                                <url>https://repo.company.net/maven</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """
        ), ctx);

        MavenSettings.Server server = settings.getServers().getServers().getFirst();
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isEqualTo("X-JFrog-Art-Api");
    }

    @Test
    void canDeserializeSettingsCorrectly() throws Exception {
        Xml.Document parsed = (Xml.Document) XmlParser.builder().build().parse("""
          <settings>
            <servers>
              <server>
                <id>maven-snapshots</id>
                <configuration>
                  <httpHeaders>
                    <property>
                      <name>X-JFrog-Art-Api</name>
                      <value>myApiToken</value>
                    </property>
                  </httpHeaders>
                  <timeout>10000</timeout>
                </configuration>
              </server>
            </servers>
          </settings>
          """).findFirst().get();

        MavenSettings.HttpHeader httpHeader = new MavenSettings.HttpHeader("X-JFrog-Art-Api", "myApiToken");
        MavenSettings.ServerConfiguration configuration = new MavenSettings.ServerConfiguration(singletonList(httpHeader), 10000L);
        MavenSettings.Server server = new MavenSettings.Server("maven-snapshots", null, null, configuration);
        MavenSettings.Servers servers = new MavenSettings.Servers(singletonList(server));
        MavenSettings settings = new MavenSettings(null, null, null, null, servers);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MavenXmlMapper.writeMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
          .writerWithDefaultPrettyPrinter()
          .writeValue(baos, settings);

        assertThat(XmlParser.builder().build().parse(baos.toString()).findFirst())
          .isPresent()
          .get(InstanceOfAssertFactories.type(Xml.Document.class))
          .isNotNull()
          .satisfies(serialized -> assertThat(SemanticallyEqual.areEqual(parsed, serialized)).isTrue())
          .satisfies(serialized -> assertThat(serialized.printAll().replace("\r", "")).isEqualTo(parsed.printAll()));
    }

    private void createSettingsSecurityFile() throws IOException {
        // Set up a temporary directory to simulate the .m2 directory
        System.setProperty("user.home", tempDir.toString());
        Files.createDirectories(tempDir.resolve(".m2"));

        Files.writeString(tempDir.resolve(".m2/settings-security.xml"),
          //language=xml
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <settingsSecurity>
                <master>%s</master>
            </settingsSecurity>
            """.formatted(MASTER_PASS_ENCRYPTED));
    }
}
