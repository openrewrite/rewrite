package org.openrewrite.maven.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawPom;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class MavenResolutionResultTest {
    @Nested
    class ResolveDependencies {

        @Nested
        // This is the strategy that Maven normally uses
        class NearestWins {

            @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.13.0,2.13.0"})
            @ParameterizedTest
            void latestMentionedWinsResolution(String firstVersion, String secondVersion) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEAREST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
            }

            @CsvSource(value = {"[2.13.0,2.15.1);[2.10.0,2.13.0];2.13.0", "(2.10.0,2.13.0];[1.0.0,2.15.1);2.15.0", "[2.13.0];[2.13.0];2.13.0"}, delimiter = ';')
            @ParameterizedTest
            void canHandleRanges(String firstVersion, String secondVersion, String expected) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEAREST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(expected);
            }

            @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.13.0,2.13.0"})
            @ParameterizedTest
            void canHandlePropertyVersions(String firstVersion, String secondVersion) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                      <jackson.version.first>%s</jackson.version.first>
                      <jackson.version.second>%s</jackson.version.second>
                    </properties>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>${jackson.version.first}</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>${jackson.version.second}</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEAREST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
            }

            @Test
            void depth0First() {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <!-- Transitively brings 2.12.2 -->
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.12.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """, ResolutionStrategy.NEAREST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.12.0");
            }

            @Test
            void shallowestFirst() {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <!-- Transitively brings 2.12.2 at depth 1 -->
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                      </dependency>
                      <dependency>
                        <!-- Transitively brings 2.17.3 at depth 3 -->
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>3.3.9</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """, ResolutionStrategy.NEAREST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-annotations", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.12.2");
            }
        }

        @Nested
        // This is the strategy that Gradle normally uses, but we're using maven poms to have reusability in the tests as they internally map to the same Dependency classes
        class NewestWins {

            @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.15.0,2.15.0"})
            @ParameterizedTest
            void newestWinsResolution(String firstVersion, String secondVersion) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEWEST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.15.0");
            }

            @CsvSource(value = {"[2.13.0,2.15.1);[2.10.0,2.13.0];2.15.0", "(2.10.0,2.13.0];[1.0.0,2.15.1);2.15.0", "[2.13.0];[2.13.0];2.13.0"}, delimiter = ';')
            @ParameterizedTest
            void canHandleRanges(String firstVersion, String secondVersion, String expected) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>%s</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEWEST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(expected);
            }

            @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.15.0,2.15.0"})
            @ParameterizedTest
            void canHandlePropertyVersions(String firstVersion, String secondVersion) {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                      <jackson.version.first>%s</jackson.version.first>
                      <jackson.version.second>%s</jackson.version.second>
                    </properties>
                  
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>${jackson.version.first}</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>${jackson.version.second}</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(firstVersion, secondVersion), ResolutionStrategy.NEWEST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.15.0");
            }

            @Test
            void transitiveOverDirect() {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <!-- Transitively brings 2.12.2 -->
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                      </dependency>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.12.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """, ResolutionStrategy.NEWEST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.12.2");
            }

            @Test
            void newestVersionWins() {
                MavenResolutionResult result = resolvePom("""
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  
                    <dependencies>
                      <dependency>
                        <!-- Transitively brings 2.12.2 at depth 1 -->
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                      </dependency>
                      <dependency>
                        <!-- Transitively brings 2.17.3 at depth 3 -->
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>3.3.9</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """, ResolutionStrategy.NEWEST_WINS);

                assertThat(result.findDependencies("com.fasterxml.jackson.core", "jackson-annotations", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly("2.17.3");
            }
        }

        private MavenResolutionResult resolvePom(@Language("xml") String pomContent, ResolutionStrategy resolutionStrategy, String... activeProfiles) {
            return resolvePom(pomContent, resolutionStrategy, emptyMap(), activeProfiles);
        }

        private MavenResolutionResult resolvePom(@Language("xml") String pomContent, ResolutionStrategy resolutionStrategy, Map<String, String> properties, String... activeProfiles) {
            try {
                ExecutionContext ctx = new InMemoryExecutionContext();
                Pom pom = RawPom.parse(new ByteArrayInputStream(pomContent.getBytes()), null).toPom(null, null);

                if (pom.getProperties().isEmpty()) {
                    pom = pom.withProperties(new LinkedHashMap<>());
                }
                pom.getProperties().putAll(properties);

                MavenPomDownloader downloader = new MavenPomDownloader(singletonMap(null, pom), ctx);

                MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
                MavenSettings sanitizedSettings = mavenCtx.getSettings() == null ? null : mavenCtx.getSettings().withServers(null);
                List<String> effectivelyActiveProfiles = Arrays.stream(activeProfiles).toList();

                ResolvedPom resolvedPom = pom.resolve(effectivelyActiveProfiles, downloader, ctx);
                return new MavenResolutionResult(randomId(), null, resolvedPom, emptyList(), null, emptyMap(), sanitizedSettings, effectivelyActiveProfiles, properties)
                  .resolveDependencies(downloader, resolutionStrategy, ctx);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to resolve POM", e);
            }
        }
    }
}