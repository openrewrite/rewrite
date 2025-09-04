/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeTransitiveDependencyVersion(
          "com.fasterxml*", "jackson-core", "2.12.5", null, null, null, null, null, null, null));
    }

    @DocumentExample
    @Test
    void singleProject() {
        rewriteRun(
          pomXml(
                """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite</groupId>
                <artifactId>core</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite</groupId>
                <artifactId>core</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.fasterxml.jackson.core</groupId>
                            <artifactId>jackson-core</artifactId>
                            <version>2.12.5</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java</artifactId>
                        <version>7.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void leavesDirectDependencyUntouched() {
        rewriteRun(
          pomXml(
                """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite</groupId>
                <artifactId>core</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.12.0</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void canUseAnyWildcardForMultipleMatchingArtifactIds() {
        rewriteRun(spec ->
            spec.recipe(new UpgradeTransitiveDependencyVersion(
              "org.apache.tomcat.embed", "*", "10.1.42", null, null, null, null, null, null, null)),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>core</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-tomcat</artifactId>
                          <version>3.3.12</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>core</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.tomcat.embed</groupId>
                              <artifactId>tomcat-embed-core</artifactId>
                              <version>10.1.42</version>
                          </dependency>
                          <dependency>
                              <groupId>org.apache.tomcat.embed</groupId>
                              <artifactId>tomcat-embed-el</artifactId>
                              <version>10.1.42</version>
                          </dependency>
                          <dependency>
                              <groupId>org.apache.tomcat.embed</groupId>
                              <artifactId>tomcat-embed-websocket</artifactId>
                              <version>10.1.42</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-tomcat</artifactId>
                          <version>3.3.12</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * This test verifies that the recipe correctly prevents downgrading transitive dependencies.
     * <ul>
     * <li>jackson-dataformat-xml:2.18.0 brings in jackson-databind:2.18.0 transitively.</li>
     * <li>jackson-datatype-jsr310:2.15.3 brings in jackson-databind:2.15.3 transitively.</li>
     * </ul>
     *  When trying to "upgrade" to 2.15.0 (which would be a downgrade), the recipe should
     *  make no changes to avoid downgrading the transitive dependency.
     */
    @Test
    void doesNotDowngradeTransitiveDependencyWithExplicitVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion(
            "com.fasterxml.jackson.core", "jackson-databind", "2.15.0", null, null, null, null, null, null, null)),
          pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite</groupId>
                <artifactId>core</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>com.fasterxml.jackson.dataformat</groupId>
                        <artifactId>jackson-dataformat-xml</artifactId>
                        <version>2.18.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.datatype</groupId>
                        <artifactId>jackson-datatype-jsr310</artifactId>
                        <version>2.15.3</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }
}
