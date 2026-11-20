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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class InheritedManagedPluginVersionTest implements RewriteTest {
    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <parent>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>mojo-parent</artifactId>
              <version>87</version>
          </parent>
          <artifactId>java25-plugin</artifactId>
          <build>
              <plugins>
                  <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-plugin-plugin</artifactId>
                  </plugin>
              </plugins>
          </build>
      </project>
      """;

    private static UpgradePluginVersion recipe(Boolean trustParent, String version) {
        return new UpgradePluginVersion("org.apache.maven.plugins", "maven-plugin-plugin", version, null, trustParent, true);
    }

    @Test
    void overridesInheritedManagedVersionWhenExplicitlyRequested() {
        rewriteRun(spec -> spec.recipe(recipe(false, "3.15.2")), pomXml(POM, spec -> spec.after(after -> {
            assertThat(after).containsPattern("<artifactId>maven-plugin-plugin</artifactId>\\s*<version>3[.]15[.]2</version>");
            return after;
        })));
    }

    @Test
    void preservesInheritedVersionByDefault() {
        rewriteRun(spec -> spec.recipe(recipe(null, "3.15.2")), pomXml(POM));
    }

    @Test
    void preservesTrustedParentVersion() {
        rewriteRun(spec -> spec.recipe(recipe(true, "3.15.2")), pomXml(POM));
    }

    @Test
    void doesNotDowngradeNewerManagedVersion() {
        rewriteRun(spec -> spec.recipe(recipe(false, "3.14.0")), pomXml(POM));
    }

    @Test
    void updatesLocalManagementWithoutAddingRedundantChildVersion() {
        String managed = POM.replace("<plugins>", "<pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins</groupId>" +
                "<artifactId>maven-plugin-plugin</artifactId><version>3.15.1</version></plugin></plugins></pluginManagement><plugins>");
        rewriteRun(spec -> spec.recipe(recipe(false, "3.15.2")), pomXml(managed, spec -> spec.after(after -> {
            assertThat(after.split("<version>3[.]15[.]2</version>", -1)).hasSize(2);
            assertThat(after).containsPattern("<artifactId>maven-plugin-plugin</artifactId>\\s*</plugin>");
            return after;
        })));
    }

    @Test
    void preservesMissingVersionWhenInsertionDisabled() {
        rewriteRun(spec -> spec.recipe(new UpgradePluginVersion("org.apache.maven.plugins", "maven-plugin-plugin", "3.15.2", null, false, false)), pomXml(POM));
    }
    @Test
    void localActiveProfileManagementDoesNotAddRedundantChildVersion() {
        String profile = POM.replace("<build>", "<profiles><profile><id>local</id><activation><activeByDefault>true</activeByDefault></activation><build>")
          .replace("</build>", "</build></profile></profiles>");
        String managed = profile.replace("<plugins>", "<pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins</groupId>" +
          "<artifactId>maven-plugin-plugin</artifactId><version>3.15.1</version></plugin></plugins></pluginManagement><plugins>");
        rewriteRun(spec -> spec.recipe(recipe(false, "3.15.2")), pomXml(managed, spec -> spec.after(after -> {
            assertThat(after.split("<version>3[.]15[.]2</version>", -1)).hasSize(2);
            assertThat(after).containsPattern("<artifactId>maven-plugin-plugin</artifactId>\\s*</plugin>");
            return after;
        })));
    }

    @Test
    void preservesNewerPluginInAnotherProfile() {
        String before = POM.replace("</project>", """
          <profiles><profile><id>future</id><build><plugins><plugin>
            <groupId>org.apache.maven.plugins</groupId><artifactId>maven-plugin-plugin</artifactId>
            <version>4.0.0-beta-1</version>
          </plugin></plugins></build></profile></profiles></project>
          """.stripTrailing());
        rewriteRun(spec -> spec.recipe(recipe(false, "3.15.2")), pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<version>4.0.0-beta-1</version>").contains("<version>3.15.2</version>");
            return after;
        })));
    }
}
