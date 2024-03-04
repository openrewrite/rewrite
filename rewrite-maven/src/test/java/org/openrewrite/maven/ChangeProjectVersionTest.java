/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ChangeProjectVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeProjectVersion("org.openrewrite", "rewrite-maven", "8.4.2", null));
    }

    @Test
    void changeProjectVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.1</version>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>${rewrite.version}</version>
                  
                  <properties>
                      <rewrite.version>8.4.1</rewrite.version>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>${rewrite.version}</version>
                  
                  <properties>
                      <rewrite.version>8.4.2</rewrite.version>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionResolveProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.1</version>
                  
                  <properties>
                      <rewrite.groupId>org.openrewrite</rewrite.groupId>
                      <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>${rewrite.groupId}</groupId>
                  <artifactId>${rewrite-maven.artifactId}</artifactId>
                  <version>8.4.2</version>
                  
                  <properties>
                      <rewrite.groupId>org.openrewrite</rewrite.groupId>
                      <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void changeProjectVersionResolvePropertiesOnParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-core</artifactId>
                  <version>8.4.1</version>
                  
                  <properties>
                      <rewrite.groupId>org.openrewrite</rewrite.groupId>
                      <rewrite-maven.artifactId>rewrite-maven</rewrite-maven.artifactId>
                  </properties>
                  
                  <packaging>pom</packaging>
              </project>
              """
          ),
          mavenProject("rewrite-maven",
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                        <version>8.4.1</version>
                    </parent>
                    
                    <groupId>${rewrite.groupId}</groupId>
                    <artifactId>${rewrite-maven.artifactId}</artifactId>
                    <version>8.4.1</version>
                </project>
                """,
              """
                <project>
                    <parent>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                        <version>8.4.1</version>
                    </parent>
                    
                    <groupId>${rewrite.groupId}</groupId>
                    <artifactId>${rewrite-maven.artifactId}</artifactId>
                    <version>8.4.2</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void doNotChangeOtherProjectVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.1</version>
              </project>
              """
          )
        );
    }

    @Test
    void changesMultipleMatchingProjects() {
        rewriteRun(
          spec -> spec.recipe(new ChangeProjectVersion("org.openrewrite", "rewrite-*", "8.4.2", null)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.1</version>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          ),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.1</version>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-gradle</artifactId>
                  <version>8.4.2</version>
              </project>
              """
          )
        );
    }

    @Test
    void doNotChangeProjectVersionInheritedFromParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-core</artifactId>
                  <version>8.4.1</version>
                  
                  <packaging>pom</packaging>
              </project>
              """
          ),
          mavenProject("rewrite-maven",
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                        <version>8.4.1</version>
                    </parent>
                    
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite-maven</artifactId>
                </project>
                """
            )
          )
        );
    }

    @Test
    void changeProjectVersionInheritedFromParentIfOverrideParentVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeProjectVersion("org.openrewrite", "rewrite-maven", "8.4.2", true)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-core</artifactId>
                  <version>8.4.1</version>
                  
                  <packaging>pom</packaging>
              </project>
              """
          ),
          mavenProject("rewrite-maven",
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                        <version>8.4.1</version>
                    </parent>
                    
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite-maven</artifactId>
                </project>
                """,
              """
                <project>
                    <parent>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                        <version>8.4.1</version>
                    </parent>
                    
                    <groupId>org.openrewrite</groupId>
                    <artifactId>rewrite-maven</artifactId>
                    <version>8.4.2</version>
                </project>
                """
            )
          )
        );
    }

}
