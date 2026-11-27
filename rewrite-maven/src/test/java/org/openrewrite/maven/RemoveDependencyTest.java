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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class RemoveDependencyTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("junit", "junit", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void noDependencyToRemove() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("junit", "junit", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeSystemDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("com.example.legacy", "local-lib", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.example.legacy</groupId>
                    <artifactId>local-lib</artifactId>
                    <version>1.0</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/local-lib.jar</systemPath>
                  </dependency>
                  <dependency>
                    <groupId>com.example.legacy</groupId>
                    <artifactId>other-lib</artifactId>
                    <version>1.0</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/other-lib.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.example.legacy</groupId>
                    <artifactId>other-lib</artifactId>
                    <version>1.0</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/other-lib.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(doc -> {
                MavenResolutionResult mavenModel = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(mavenModel.getPom().getRequestedDependencies())
                  .singleElement().satisfies(dependency -> assertThat(dependency.getArtifactId()).isEqualTo("other-lib"));
            })
          )
        );
    }

    @Test
    void removeDependencyWithManagedSystemScopeAndPropertyCoordinates() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("com.example.legacy", "local-lib", "system")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <properties>
                  <legacy.group>com.example.legacy</legacy.group>
                  <legacy.artifact>local-lib</legacy.artifact>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.example.legacy</groupId>
                      <artifactId>local-lib</artifactId>
                      <version>1.0</version>
                      <scope>system</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>${legacy.group}</groupId>
                    <artifactId>${legacy.artifact}</artifactId>
                    <systemPath>${project.basedir}/lib/local-lib.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <properties>
                  <legacy.group>com.example.legacy</legacy.group>
                  <legacy.artifact>local-lib</legacy.artifact>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.example.legacy</groupId>
                      <artifactId>local-lib</artifactId>
                      <version>1.0</version>
                      <scope>system</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void keepSystemDependencyWhenScopeDiffers() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("com.example.legacy", "local-lib", "compile")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.example.legacy</groupId>
                    <artifactId>local-lib</artifactId>
                    <version>1.0</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/local-lib.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeSystemDependencyFromProfile() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("com.example.legacy", "local-lib", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>legacy</id>
                    <dependencies>
                      <dependency>
                        <groupId>com.example.legacy</groupId>
                        <artifactId>local-lib</artifactId>
                        <version>1.0</version>
                        <scope>system</scope>
                        <systemPath>${project.basedir}/lib/local-lib.jar</systemPath>
                      </dependency>
                    </dependencies>
                  </profile>
                </profiles>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <profiles>
                  <profile>
                    <id>legacy</id>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }

    @Test
    void shouldRemoveScopedDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.junit.jupiter", "junit-jupiter", "compile")),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                         <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <scope>test</scope>
                      </dependency>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter-api</artifactId>
                          <version>5.6.3</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                         <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <scope>test</scope>
                      </dependency>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter-api</artifactId>
                          <version>5.6.3</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeDependencyMatchedByPatternWithScopeFromDependencyManagement() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.junit.jupiter", "junit-jupiter*", null)),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                              <scope>test</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                              <scope>test</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/422")
    @Test
    void removeDependencyByEffectiveScope() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("junit", "junit", "runtime")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void updateModelWhenAllDependenciesRemoved() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("com.google.guava", "guava", null)),
          pomXml(
            """
                  <project>
                     <modelVersion>4.0.0</modelVersion>
                     <groupId>foo</groupId>
                     <artifactId>bar</artifactId>
                     <version>0.0.1-SNAPSHOT</version>
                     <dependencies>
                         <dependency>
                             <groupId>com.google.guava</groupId>
                             <artifactId>guava</artifactId>
                             <version>29.0-jre</version>
                             <type>pom</type>
                         </dependency>
                     </dependencies>
                 </project>
              """,
            """
                  <project>
                     <modelVersion>4.0.0</modelVersion>
                     <groupId>foo</groupId>
                     <artifactId>bar</artifactId>
                     <version>0.0.1-SNAPSHOT</version>
                 </project>
              """,
            spec -> spec.afterRecipe(doc -> {
                MavenResolutionResult mavenModel = doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .orElseThrow(() -> new IllegalStateException("The maven must should exist on the document."));
                assertThat(mavenModel.getDependencies().get(Scope.Compile)).isEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5117")
    @Test
    void removeDependencyWithTypeZip() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.elasticsearch.distribution.integ-test-zip", "elasticsearch", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>org.elasticsearch.distribution.integ-test-zip</groupId>
                    <artifactId>elasticsearch</artifactId>
                    <version>7.17.15</version>
                    <type>zip</type>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4796")
    void removeDependencyDefinedWithProperties() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("junit", "junit", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <junit.groupId>junit</junit.groupId>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>${junit.groupId}</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <junit.groupId>junit</junit.groupId>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
