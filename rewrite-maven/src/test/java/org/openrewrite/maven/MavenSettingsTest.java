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

import org.assertj.core.api.Condition;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryMirror;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"HttpUrlsUsage", "ConstantConditions"})
class MavenSettingsTest {

    @Test
    void parse() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));

        assertThat(ctx.getRepositories()).hasSize(1);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    void defaultActiveWhenNoOthersAreActive() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));

        assertThat(ctx.getRepositories().stream().map(MavenRepository::getUri)).containsExactly("https://activebydefault.com");
    }

    @Test
    void idCollisionLastRepositoryWins() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));

        assertThat(ctx.getRepositories())
          .as("When multiple repositories have the same id in a maven settings file the last one wins. In a pom.xml an error would be thrown.")
          .containsExactly(new MavenRepository("repo", "https://lastwins.com", null, null, null, null));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/131")
    @Test
    void defaultOnlyActiveIfNoOthersAreActive() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));


        assertThat(ctx.getActiveProfiles())
          .containsExactly("repo");

        assertThat(ctx.getRepositories().stream().map(MavenRepository::getUri))
          .containsExactly("https://activebyactivationlist.com");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/130")
    @Test
    void mirrorReplacesRepository() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));

        assertThat(ctx.getRepositories().stream()
          .map(repo -> MavenRepositoryMirror.apply(ctx.getMirrors(), repo))
          .map(MavenRepository::getUri))
          .containsExactly("https://internalartifactrepository.yourorg.com");
    }

    @Test
    void starredMirrorWithExclusion() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx));

        assertThat(ctx.getRepositories().stream()
          .map(repo -> MavenRepositoryMirror.apply(ctx.getMirrors(), repo)))
          .hasSize(2)
          .haveAtLeastOne(
            new Condition<>(repo -> repo.getUri().equals("https://internalartifactrepository.yourorg.com"),
              "Repository should-be-mirrored should have had its URL changed to https://internalartifactrepository.yourorg.com"
            )
          ).haveAtLeastOne(
            new Condition<>(repo -> repo.getUri().equals("https://externalrepository.com") &&
                                    "should-not-be-mirrored".equals(repo.getId()),
              "Repository should-not-be-mirrored should have had its URL left unchanged"
            )
          );
    }

    @Test
    void serverCredentials() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        var settings = MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), ctx);

        assertThat(settings.getServers()).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1);
        assertThat(settings.getServers().getServers().get(0))
          .matches(repo -> repo.getId().equals("server001"))
          .matches(repo -> repo.getUsername().equals("my_login"))
          .matches(repo -> repo.getPassword().equals("my_password"));
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite/issues/1688")
    class LocalRepositoryTest {
        @Test
        void parsesLocalRepositoryPathFromSettingsXml() {
            var localRepoPath = System.getProperty("java.io.tmpdir");
            var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
            ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
              //language=xml
              """
                    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                          <localRepository>%s</localRepository>
                    </settings>
                """.formatted(localRepoPath).getBytes()
            )), ctx));
            assertThat(ctx.getLocalRepository().getUri())
              .startsWith("file://")
              .containsSubsequence(Paths.get(localRepoPath).toUri().toString().split("/"));
        }

        @Test
        void parsesLocalRepositoryUriFromSettingsXml() {
            var localRepoPath = Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString();
            var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
            ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
              //language=xml
              """
                    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                          <localRepository>%s</localRepository>
                    </settings>
                """.formatted(localRepoPath).getBytes()
            )), ctx));

            assertThat(ctx.getLocalRepository().getUri())
              .startsWith("file://")
              .containsSubsequence(localRepoPath.split("/"));
        }

        @Test
        void defaultsToTheMavenDefault() {
            var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
            ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
              //language=xml
              """
                        <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                        </settings>
                """.getBytes()
            )), ctx));

            assertThat(ctx.getLocalRepository().getUri()).isEqualTo(MavenRepository.MAVEN_LOCAL_DEFAULT.getUri());
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1801")
    @Nested
    class InterpolationTest {
        @Test
        void properties() {
            System.setProperty("rewrite.test.custom.location", "/tmp");
            var settings = MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
                """.getBytes()
            )), new InMemoryExecutionContext());

            assertThat(settings.getLocalRepository()).isEqualTo("/tmp/maven/local/repository/");
        }

        @Test
        void unresolvedPlaceholdersRemainUnchanged() {
            var settings = MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
                """.getBytes()
            )), new InMemoryExecutionContext());

            assertThat(settings.getLocalRepository())
              .isEqualTo("${custom.location.zz}/maven/local/repository/");
            assertThat(settings.getServers().getServers().get(0).getUsername()).isEqualTo("${env.PRIVATE_REPO_USERNAME_ZZ}");
            assertThat(settings.getServers().getServers().get(0).getPassword()).isEqualTo("${env.PRIVATE_REPO_PASSWORD_ZZ}");
        }

        @Test
        @Disabled("Depends on methods which are unstable in CI build")
        void env() {
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_USERNAME", "user");
            updateEnvMap("REWRITE_TEST_PRIVATE_REPO_PASSWORD", "pass");
            var settings = MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
                """.getBytes()
            )), new InMemoryExecutionContext());

            assertThat(settings.getServers()).isNotNull();
            assertThat(settings.getServers().getServers()).hasSize(1);
            assertThat(settings.getServers().getServers().get(0).getUsername()).isEqualTo("user");
            assertThat(settings.getServers().getServers().get(0).getPassword()).isEqualTo("pass");
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
              <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
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
            Path path = Paths.get("settings.xml");
            var baseSettings = MavenSettings.parse(new Parser.Input(path, () -> new ByteArrayInputStream(
              installationSettings.getBytes())), new InMemoryExecutionContext());
            var userSettings = MavenSettings.parse(new Parser.Input(path, () -> new ByteArrayInputStream(
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
                """.getBytes()
            )), new InMemoryExecutionContext());

            var mergedSettings = userSettings.merge(baseSettings);

            assertThat(mergedSettings.getProfiles().getProfiles()).hasSize(2);
            assertThat(mergedSettings.getActiveProfiles().getActiveProfiles()).hasSize(2);
            assertThat(mergedSettings.getMirrors().getMirrors()).hasSize(2);
            assertThat(mergedSettings.getServers().getServers()).hasSize(2);
        }

        @Test
        void replacesElementsWithMatchingIds() {
            Path path = Paths.get("settings.xml");
            var baseSettings = MavenSettings.parse(new Parser.Input(path, () -> new ByteArrayInputStream(
              installationSettings.getBytes())), new InMemoryExecutionContext());
            var userSettings = MavenSettings.parse(new Parser.Input(path, () -> new ByteArrayInputStream(
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
                """.getBytes()
            )), new InMemoryExecutionContext());

            var mergedSettings = userSettings.merge(baseSettings);

            assertThat(mergedSettings.getProfiles().getProfiles()).hasSize(1);
            assertThat(mergedSettings.getProfiles().getProfiles().get(0).getRepositories().getRepositories().get(0).getSnapshots()).isNull();
            assertThat(mergedSettings.getActiveProfiles().getActiveProfiles()).hasSize(1);
            assertThat(mergedSettings.getMirrors().getMirrors()).hasSize(1);

            assertThat(mergedSettings.getMirrors().getMirrors().get(0).getUrl())
              .isEqualTo("http://downloads.planetmirror.com/pub/maven3000");

            assertThat(mergedSettings.getServers().getServers()).hasSize(1);
            assertThat(mergedSettings.getServers().getServers().get(0))
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
        var settings = MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
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
            """.getBytes()
        )), new InMemoryExecutionContext());

        MavenSettings.Server server = settings.getServers().getServers().get(0);
        assertThat(server.getConfiguration().getHttpHeaders().get(0).getName()).isEqualTo("X-JFrog-Art-Api");
    }
}
