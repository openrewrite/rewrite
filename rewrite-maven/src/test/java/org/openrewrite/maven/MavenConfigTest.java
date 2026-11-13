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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class MavenConfigTest {

    @TempDir
    Path mavenRoot;

    @ParameterizedTest
    @ValueSource(strings = {
      "-Pfoo",
      "-P foo",
      "-P\tfoo",
      "--activate-profiles=foo",
      "--activate-profiles foo",
      "-P\nfoo"
    })
    void everyFormOfActivatingAProfile(String config) {
        assertThat(MavenConfig.parse(config).getActiveProfiles()).containsExactly("foo");
    }

    @Test
    void commaSeparatedProfiles() {
        assertThat(MavenConfig.parse("-Pfoo,bar,baz").getActiveProfiles())
          .containsExactly("foo", "bar", "baz");
    }

    @Test
    void whitespaceAroundCommaSeparatedProfiles() {
        assertThat(MavenConfig.parse("--activate-profiles='foo, bar'").getActiveProfiles())
          .containsExactly("foo", "bar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-P!foo", "-P-foo"})
    void deactivatedProfilesAreNotActive(String mavenConfig) {
        MavenConfig config = MavenConfig.parse(mavenConfig);
        assertThat(config.getActiveProfiles()).isEmpty();
        assertThat(config.getInactiveProfiles()).containsExactly("foo");
    }

    @Test
    void activatedAndDeactivatedProfilesInOneOption() {
        MavenConfig config = MavenConfig.parse("-Pfoo,!bar,baz");
        assertThat(config.getActiveProfiles()).containsExactly("foo", "baz");
        assertThat(config.getInactiveProfiles()).containsExactly("bar");
    }

    @Test
    void repeatedProfileOptionsAccumulate() {
        assertThat(MavenConfig.parse("-Pfoo\n-Pbar").getActiveProfiles())
          .containsExactly("foo", "bar");
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "-Dfoo=bar",
      "-D foo=bar",
      "--define=foo=bar",
      "--define foo=bar"
    })
    void everyFormOfDefiningAProperty(String config) {
        assertThat(MavenConfig.parse(config).getProperties()).containsExactly(entry("foo", "bar"));
    }

    @Test
    void propertyWithoutAValueIsTrue() {
        assertThat(MavenConfig.parse("-Dskip.tests").getProperties())
          .containsExactly(entry("skip.tests", "true"));
    }

    @Test
    void propertyValueSplitsOnTheFirstEqualsSign() {
        assertThat(MavenConfig.parse("-Dexclude=**/*Test.java=skip").getProperties())
          .containsExactly(entry("exclude", "**/*Test.java=skip"));
    }

    @Test
    void quotedPropertyValueMayContainSpaces() {
        assertThat(MavenConfig.parse("-Dmessage=\"hello world\" -Pfoo").getProperties())
          .containsExactly(entry("message", "hello world"));
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        assertThat(MavenConfig.parse("""
          # activate the corporate profile
          -Pcorp

          # a comment that mentions -Pnot-a-real-profile
          -T1C
          """).getActiveProfiles()).containsExactly("corp");
    }

    @Test
    void unrecognizedOptionsDoNotDiscardRecognizedOnes() {
        MavenConfig config = MavenConfig.parse("-T 1C --no-transfer-progress -Pcorp -e -Dfoo=bar");
        assertThat(config.getActiveProfiles()).containsExactly("corp");
        assertThat(config.getProperties()).containsExactly(entry("foo", "bar"));
    }

    @Test
    void optionsSpreadOverLinesAndWithinLines() {
        // Maven 3.9+ reads one argument per line; older versions split the whole file on whitespace
        MavenConfig config = MavenConfig.parse("""
          --batch-mode
          -Pcorp,release
          -Dmaven.compiler.release=17 -Dfoo=bar
          """);
        assertThat(config.getActiveProfiles()).containsExactly("corp", "release");
        assertThat(config.getProperties())
          .containsExactly(entry("maven.compiler.release", "17"), entry("foo", "bar"));
    }

    @Test
    void carriageReturnsAreNotPartOfTheValue() {
        assertThat(MavenConfig.parse("-Pcorp\r\n-Dfoo=bar\r\n").getActiveProfiles())
          .containsExactly("corp");
    }

    @Test
    void optionAtEndOfFileWithNoValue() {
        assertThat(MavenConfig.parse("-Dfoo=bar\n-P").getActiveProfiles()).isEmpty();
    }

    @Test
    void nothingToParse() {
        assertThat(MavenConfig.parse("")).isSameAs(MavenConfig.EMPTY);
        assertThat(MavenConfig.parse("-T1C")).isSameAs(MavenConfig.EMPTY);
    }

    @Test
    void readsFromTheMavenRoot() throws IOException {
        Files.createDirectories(mavenRoot.resolve(".mvn"));
        Files.writeString(mavenRoot.resolve(".mvn/maven.config"), "-Pcorp");

        assertThat(MavenConfig.read(mavenRoot, new InMemoryExecutionContext()).getActiveProfiles())
          .containsExactly("corp");
    }

    @Test
    void noMavenConfigOnDisk() {
        assertThat(MavenConfig.read(mavenRoot, new InMemoryExecutionContext())).isSameAs(MavenConfig.EMPTY);
    }

    /**
     * Maven reads maven.config from the directory it was invoked in alone, so a config belonging to a
     * subproject must not leak into a parse rooted above it.
     */
    @Test
    void mavenConfigInASubdirectoryIsNotRead() throws IOException {
        Files.createDirectories(mavenRoot.resolve("submodule/.mvn"));
        Files.writeString(mavenRoot.resolve("submodule/.mvn/maven.config"), "-Pcorp");

        assertThat(MavenConfig.read(mavenRoot, new InMemoryExecutionContext())).isSameAs(MavenConfig.EMPTY);
    }

    /**
     * The point of parsing the file at all: a property that only a maven.config-activated profile
     * contributes has to reach the resolved pom, or the parse sees a project the build never builds.
     */
    @Test
    void profilesAndPropertiesReachTheResolvedPom() {
        @Language("xml")
        String pom = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.corp</groupId>
              <artifactId>app</artifactId>
              <version>1.0</version>
              <profiles>
                  <profile>
                      <id>corp</id>
                      <properties>
                          <maven.compiler.release>17</maven.compiler.release>
                      </properties>
                  </profile>
              </profiles>
          </project>
          """;

        assertThat(resolveProperties(MavenParser.builder(), pom))
          .doesNotContainKey("maven.compiler.release");

        assertThat(resolveProperties(
          MavenParser.builder().mavenConfig(MavenConfig.parse("-Pcorp -Dfoo=bar")), pom))
          .containsEntry("maven.compiler.release", "17")
          .containsEntry("foo", "bar");
    }

    /**
     * The reason deactivation is modeled at all: a profile the pom activates on its own cannot be turned off
     * by leaving it out of the active profile list.
     */
    @Test
    void deactivatedProfilesDoNotReachTheResolvedPom() {
        @Language("xml")
        String pom = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.corp</groupId>
              <artifactId>app</artifactId>
              <version>1.0</version>
              <profiles>
                  <profile>
                      <id>corp</id>
                      <activation>
                          <activeByDefault>true</activeByDefault>
                      </activation>
                      <properties>
                          <maven.compiler.release>17</maven.compiler.release>
                      </properties>
                  </profile>
              </profiles>
          </project>
          """;

        assertThat(resolveProperties(MavenParser.builder(), pom))
          .containsEntry("maven.compiler.release", "17");

        assertThat(resolveProperties(
          MavenParser.builder().mavenConfig(MavenConfig.parse("-P!corp")), pom))
          .doesNotContainKey("maven.compiler.release");
    }

    @Test
    void deactivationWinsOverActivationOfTheSameProfile() {
        @Language("xml")
        String pom = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.corp</groupId>
              <artifactId>app</artifactId>
              <version>1.0</version>
              <profiles>
                  <profile>
                      <id>corp</id>
                      <properties>
                          <maven.compiler.release>17</maven.compiler.release>
                      </properties>
                  </profile>
              </profiles>
          </project>
          """;

        assertThat(resolveProperties(
          MavenParser.builder().mavenConfig(MavenConfig.parse("-Pcorp,!corp")), pom))
          .doesNotContainKey("maven.compiler.release");
    }

    /**
     * A profile that is active by default stays active when some other profile is deactivated, since
     * deactivation is not the "another profile was activated" that turns activeByDefault off.
     */
    @Test
    void deactivatingOneProfileDoesNotDeactivateAnotherActiveByDefault() {
        @Language("xml")
        String pom = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.corp</groupId>
              <artifactId>app</artifactId>
              <version>1.0</version>
              <profiles>
                  <profile>
                      <id>corp</id>
                      <activation>
                          <activeByDefault>true</activeByDefault>
                      </activation>
                      <properties>
                          <maven.compiler.release>17</maven.compiler.release>
                      </properties>
                  </profile>
                  <profile>
                      <id>release</id>
                      <properties>
                          <gpg.skip>false</gpg.skip>
                      </properties>
                  </profile>
              </profiles>
          </project>
          """;

        assertThat(resolveProperties(
          MavenParser.builder().mavenConfig(MavenConfig.parse("-P!release")), pom))
          .containsEntry("maven.compiler.release", "17")
          .doesNotContainKey("gpg.skip");
    }

    private static Map<String, String> resolveProperties(MavenParser.Builder parser, @Language("xml") String pom) {
        return parser.skipDependencyResolution(true).build()
          .parse(pom)
          .findFirst()
          .orElseThrow()
          .getMarkers()
          .findFirst(MavenResolutionResult.class)
          .orElseThrow()
          .getPom()
          .getProperties();
    }
}
