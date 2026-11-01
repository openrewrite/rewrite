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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class RetainVersionsModulesTest implements RewriteTest {
    private static final String PARENT = """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.example</groupId>
        <artifactId>parent</artifactId>
        <version>1.0</version>
        <packaging>pom</packaging>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-dependencies</artifactId>
              <version>4.1.0</version>
              <type>pom</type>
              <scope>import</scope>
            </dependency>
          </dependencies>
        </dependencyManagement>
      </project>
      """;

    private static final String CHILD = """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>org.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0</version>
        </parent>
        <artifactId>child</artifactId>
        <dependencies>
          <dependency>
            <groupId>org.codehaus.janino</groupId>
            <artifactId>janino</artifactId>
          </dependency>
        </dependencies>
      </project>
      """;

    private static String upgradedParent(String version) {
        return PARENT.replace("4.1.0", "4.2.0-RC1")
          .replace("      <dependency>", """
                  <dependency>
                    <groupId>org.codehaus.janino</groupId>
                    <artifactId>janino</artifactId>
                    <version>%s</version>
                  </dependency>
                  <dependency>""".formatted(version).indent(6).stripTrailing());
    }

    @Test
    void propertyBasedBomRetainsDirectDependencyVersion() {
        String before = PARENT.replace("org.springframework.boot", "org.springframework.cloud")
          .replace("spring-boot-dependencies", "spring-cloud-dependencies")
          .replace("  <dependencyManagement>", "  <properties>\n    <cloud.version>2023.0.2</cloud.version>\n  </properties>\n  <dependencyManagement>")
          .replace("<version>4.1.0</version>", "<version>${cloud.version}</version>")
          .replace("</project>", """
              <dependencies>
                <dependency>
                  <groupId>org.springframework.security</groupId>
                  <artifactId>spring-security-rsa</artifactId>
                </dependency>
              </dependencies>
            </project>""");
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-dependencies",
            "2025.1.4", null, true, singletonList("org.springframework.security:spring-security-rsa"))),
          pomXml(before, before.replace("<cloud.version>2023.0.2</cloud.version>", "<cloud.version>2025.1.4</cloud.version>")
            .replace("      <artifactId>spring-security-rsa</artifactId>", "      <artifactId>spring-security-rsa</artifactId>\n      <version>1.1.3</version>"))
        );
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void nestedImportedBomRetainsManagementForChild(boolean childFirst, boolean propertyVersion) {
        String parentPom = PARENT.replace("org.springframework.boot", "org.springframework.cloud")
          .replace("spring-boot-dependencies", "spring-cloud-dependencies").replace("4.1.0", "2023.0.2");
        String expected = upgradedParent("1.1.3").replace("org.springframework.boot", "org.springframework.cloud")
          .replace("spring-boot-dependencies", "spring-cloud-dependencies").replace("4.2.0-RC1", "2025.1.4")
          .replace("org.codehaus.janino", "org.springframework.security").replace("<artifactId>janino</artifactId>", "<artifactId>spring-security-rsa</artifactId>");
        if (propertyVersion) {
            parentPom = parentPom.replace("  <dependencyManagement>", "  <properties>\n    <cloud.version>2023.0.2</cloud.version>\n  </properties>\n  <dependencyManagement>")
              .replace("<version>2023.0.2</version>", "<version>${cloud.version}</version>");
            expected = expected.replace("  <dependencyManagement>", "  <properties>\n    <cloud.version>2025.1.4</cloud.version>\n  </properties>\n  <dependencyManagement>")
              .replace("<version>2025.1.4</version>", "<version>${cloud.version}</version>");
        }
        SourceSpecs parent = pomXml(parentPom, expected);
        SourceSpecs child = mavenProject("child", pomXml(CHILD.replace("org.codehaus.janino", "org.springframework.security")
          .replace("<artifactId>janino</artifactId>", "<artifactId>spring-security-rsa</artifactId>")));
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-dependencies",
            "2025.1.4", null, true, singletonList("org.springframework.security:spring-security-rsa"))),
          childFirst ? new SourceSpecs[]{child, parent} : new SourceSpecs[]{parent, child}
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parentAndChildImportingSameBomRetainRemovedManagement(boolean childFirst) {
        String childWithImport = CHILD.replace("  <dependencies>", """
            <dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-dependencies</artifactId>
                  <version>4.1.0</version>
                  <type>pom</type>
                  <scope>import</scope>
                </dependency>
              </dependencies>
            </dependencyManagement>
            <dependencies>""");
        SourceSpecs parent = pomXml(PARENT, upgradedParent("3.1.12"));
        SourceSpecs child = mavenProject("child",
          pomXml(childWithImport, childWithImport.replace("4.1.0", "4.2.0-RC1")));
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.codehaus.janino:*"))),
          childFirst ? new SourceSpecs[]{child, parent} : new SourceSpecs[]{parent, child}
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.1.11", "${janino.version}"})
    void explicitChildVersionsAreUntouched(String version) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.codehaus.janino:*"))),
          pomXml(PARENT, PARENT.replace("4.1.0", "4.2.0-RC1")),
          mavenProject("child",
            pomXml(CHILD.replace("  <dependencies>", """
                <properties>
                  <janino.version>3.1.11</janino.version>
                </properties>
                <dependencies>""")
              .replace("      <artifactId>janino</artifactId>",
                "      <artifactId>janino</artifactId>\n      <version>" + version + "</version>"))
          )
        );
    }

    @Test
    void childManagementPropertyOverrideIsUntouchedWhileSiblingNeedsRetention() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.codehaus.janino:*"))),
          pomXml(PARENT, upgradedParent("3.1.12")),
          mavenProject("child",
            pomXml(CHILD.replace("  <dependencies>", """
                <properties>
                  <janino.version>3.1.11</janino.version>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.codehaus.janino</groupId>
                      <artifactId>janino</artifactId>
                      <version>${janino.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>"""))
          ),
          mavenProject("sibling", pomXml(CHILD.replace("<artifactId>child</artifactId>", "<artifactId>sibling</artifactId>")))
        );
    }

    @Test
    void dependencyStillManagedByNewBomNeedsNoRetention() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.apache.commons:*"))),
          pomXml(PARENT, PARENT.replace("4.1.0", "4.2.0-RC1")),
          mavenProject("child",
            pomXml(CHILD.replace("org.codehaus.janino", "org.apache.commons")
              .replace("<artifactId>janino</artifactId>", "<artifactId>commons-lang3</artifactId>"))
          )
        );
    }

    @Test
    void explicitRetainedVersionForChild() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.codehaus.janino:*:3.1.11"))),
          pomXml(PARENT, upgradedParent("3.1.11")),
          mavenProject("child", pomXml(CHILD))
        );
    }

    @Test
    void descendantAndUnrelatedModule() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies",
            "4.2.0-RC1", null, true, singletonList("org.codehaus.janino:*"))),
          // Put the grandchild before the importing POM to exercise source-order independence.
          mavenProject("child/grandchild",
            pomXml(CHILD.replace("<artifactId>parent</artifactId>", "<artifactId>child</artifactId>")
              .replace("<artifactId>child</artifactId>\n  <dependencies>",
                "<artifactId>grandchild</artifactId>\n  <dependencies>"))
          ),
          mavenProject("child",
            pomXml(CHILD.substring(0, CHILD.indexOf("  <dependencies>")) + "  <packaging>pom</packaging>\n</project>")
          ),
          pomXml(PARENT, upgradedParent("3.1.12")),
          mavenProject("unrelated",
            pomXml("""
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>unrelated</artifactId>
                <version>1.0</version>
                <properties>
                  <janino.version>3.1.11</janino.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>org.codehaus.janino</groupId>
                    <artifactId>janino</artifactId>
                    <version>${janino.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """)
          )
        );
    }
}
