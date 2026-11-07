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

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.maven.RemoveRedundantDependencyVersions.Comparator.EQ;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ChildModuleModelPropagationTest implements RewriteTest {

    private static final String PARENT_BEFORE = """
      <project>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0-SNAPSHOT</version>
          <packaging>pom</packaging>
          <modules>
              <module>child</module>
          </modules>
          <dependencyManagement>
              <dependencies>
                  <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>32.1.3-jre</version>
                  </dependency>
              </dependencies>
          </dependencyManagement>
      </project>
      """;

    private static final String PARENT_AFTER = PARENT_BEFORE.replace("32.1.3-jre", "33.0.0-jre");

    private static final String JAVAX_PARENT_BEFORE = """
      <project>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0-SNAPSHOT</version>
          <packaging>pom</packaging>
          <modules>
              <module>child</module>
          </modules>
          <dependencyManagement>
              <dependencies>
                  <dependency>
                      <groupId>javax.activation</groupId>
                      <artifactId>javax.activation-api</artifactId>
                      <version>1.2.0</version>
                  </dependency>
              </dependencies>
          </dependencyManagement>
      </project>
      """;

    private static final String JAVAX_PARENT_AFTER = JAVAX_PARENT_BEFORE
      .replace("javax.activation</groupId>", "jakarta.activation</groupId>")
      .replace("javax.activation-api", "jakarta.activation-api")
      .replace("1.2.0", "2.1.0");

    @Test
    void childModelSeesManagedVersionChangedInLocalParent() {
        AtomicReference<String> managed = new AtomicReference<>();
        AtomicReference<String> resolved = new AtomicReference<>();
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.google.guava", "guava", "33.0.0-jre", null, null, null),
            toRecipe(() -> new MavenIsoVisitor<>() {
                @Override
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    if (document.getSourcePath().endsWith("child/pom.xml")) {
                        MavenResolutionResult mrr = getResolutionResult();
                        managed.set(mrr.getPom().getManagedVersion("com.google.guava", "guava", null, null));
                        mrr.getDependencies().get(Scope.Compile).stream()
                          .filter(d -> "guava".equals(d.getArtifactId()))
                          .findFirst()
                          .map(ResolvedDependency::getVersion)
                          .ifPresent(resolved::set);
                    }
                    return document;
                }
            })
          ),
          mavenProject("parent",
            pomXml(PARENT_BEFORE, PARENT_AFTER),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
        assertThat(managed.get()).as("child's managed version").isEqualTo("33.0.0-jre");
        assertThat(resolved.get()).as("child's resolved guava").isEqualTo("33.0.0-jre");
    }

    @Test
    void childExplicitVersionBecomesRedundantAfterParentManagedVersionChange() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.google.guava", "guava", "33.0.0-jre", null, null, null),
            new RemoveRedundantDependencyVersions(null, null, EQ, null)
          ),
          mavenProject("parent",
            pomXml(PARENT_BEFORE, PARENT_AFTER),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>33.0.0-jre</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void childExplicitVersionBecomesRedundantAfterParentBomImportUpgrade() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("org.springframework.boot", "spring-boot-dependencies", "3.4.0", null, null, null),
            new RemoveRedundantDependencyVersions("org.springframework.boot", "*", EQ, null)
          ),
          mavenProject("parent",
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child</module>
                    </modules>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-dependencies</artifactId>
                                <version>3.3.3</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """,
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child</module>
                    </modules>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-dependencies</artifactId>
                                <version>3.4.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """
            ),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-starter</artifactId>
                              <version>3.4.0</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-starter</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void childVisitedBeforeParentCatchesUpInNextCycle() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.google.guava", "guava", "33.0.0-jre", null, null, null),
            new RemoveRedundantDependencyVersions(null, null, EQ, null)
          ).expectedCyclesThatMakeChanges(2),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0-SNAPSHOT</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>33.0.0-jre</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0-SNAPSHOT</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          ),
          pomXml(PARENT_BEFORE, PARENT_AFTER, spec -> spec.path("pom.xml"))
        );
    }

    @Test
    void childSeesRenamedManagedDependencyEvenWhenItsOwnDependenciesNoLongerResolve() {
        AtomicReference<List<String>> managed = new AtomicReference<>();
        rewriteRun(
          spec -> spec.recipes(
            new ChangeManagedDependencyGroupIdAndArtifactId("javax.activation", "javax.activation-api",
              "jakarta.activation", "jakarta.activation-api", "2.1.0"),
            toRecipe(() -> new MavenIsoVisitor<>() {
                @Override
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    if (document.getSourcePath().endsWith("child/pom.xml")) {
                        managed.set(getResolutionResult().getPom().getDependencyManagement().stream()
                          .map(dm -> dm.getGroupId() + ":" + dm.getArtifactId() + ":" + dm.getVersion())
                          .toList());
                    }
                    return document;
                }
            })
          ),
          mavenProject("parent",
            pomXml(JAVAX_PARENT_BEFORE, JAVAX_PARENT_AFTER),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
        assertThat(managed.get())
          .contains("jakarta.activation:jakarta.activation-api:2.1.0")
          .doesNotContain("javax.activation:javax.activation-api:1.2.0");
    }

    @Test
    void childModelReflectsDependencyRenamedInParentAndChild() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyGroupIdAndArtifactId("javax.activation", "javax.activation-api",
            "jakarta.activation", "jakarta.activation-api", "2.1.0", null)),
          mavenProject("parent",
            pomXml(JAVAX_PARENT_BEFORE, JAVAX_PARENT_AFTER),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    assertThat(mrr.getPom().getManagedVersion("jakarta.activation", "jakarta.activation-api", null, null))
                      .isEqualTo("2.1.0");
                    assertThat(mrr.getDependencies().get(Scope.Compile))
                      .extracting(ResolvedDependency::getArtifactId)
                      .contains("jakarta.activation-api")
                      .doesNotContain("javax.activation-api");
                })
              )
            )
          )
        );
    }
}
