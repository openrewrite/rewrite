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
package org.openrewrite.gradle.testselection;

import org.junit.jupiter.api.Test;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.DataTableStore;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryDataTableStore;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.table.AffectedModulesDataTable;
import org.openrewrite.table.ChangedFilesDataTable;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ClassifyAffectedModulesTest implements RewriteTest {

    /**
     * Build an {@link ExecutionContext} preloaded with a {@link ChangedFilesDataTable}
     * that contains the given {@code (path, changeType)} pairs. The resulting context
     * is handed to {@code rewriteRun} so that both parsing and recipe execution share
     * the same {@link DataTableStore} — making the rows visible to the recipe and any
     * subsequent afterRecipe assertion.
     */
    private static ExecutionContext ctxWithChanges(String... pathsAndTypes) {
        if (pathsAndTypes.length % 2 != 0) {
            throw new IllegalArgumentException("Expected (path, changeType) pairs");
        }
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        ChangedFilesDataTable table = new ChangedFilesDataTable(Recipe.noop());
        for (int i = 0; i < pathsAndTypes.length; i += 2) {
            store.insertRow(table, ctx, new ChangedFilesDataTable.Row(pathsAndTypes[i], pathsAndTypes[i + 1]));
        }
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        return ctx;
    }

    private static List<AffectedModulesDataTable.Row> readAffected(DataTableStore store) {
        List<AffectedModulesDataTable.Row> rows = new ArrayList<>();
        store.getRows(AffectedModulesDataTable.class).forEach(rows::add);
        return rows;
    }

    // ---------------------------------------------------------------------
    // Maven multi-module
    // ---------------------------------------------------------------------

    @Test
    void mavenChildPomChange_emitsChildAndDependentSibling() {
        // sub-b depends on sub-a. A pom change in sub-a should flag sub-a
        // ("build-file-changed") and sub-b ("module-dep-of-affected").
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/sub-a/pom.xml", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows)
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/sub-a");
                            assertThat(r.getReason()).isEqualTo("build-file-changed");
                        })
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/sub-b");
                            assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                        });
            }),
          mavenProject("multi-project-build",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>sub-a</module>
                        <module>sub-b</module>
                    </modules>
                </project>
                """
            ),
            mavenProject("sub-a",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-a</artifactId>
                  </project>
                  """
              )
            ),
            mavenProject("sub-b",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-b</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.example</groupId>
                              <artifactId>sub-a</artifactId>
                              <version>1</version>
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
    void mavenParentPomChange_cascadesToChildren() {
        // The parent pom is at the root module (path = "multi-project-build"). Since
        // that *is* the inferred root module of this layout, a parent-pom change hits
        // the repo-root fallback list (root pom.xml) and produces a full-repo bailout.
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/pom.xml", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                // Every module discovered must appear with a repo-root-bailout reason.
                List<String> modules = rows.stream()
                        .map(AffectedModulesDataTable.Row::getModule)
                        .collect(Collectors.toList());
                assertThat(modules)
                        .contains("multi-project-build", "multi-project-build/sub-a", "multi-project-build/sub-b");
                assertThat(rows).allSatisfy(r ->
                        assertThat(r.getReason()).startsWith("repo-root-bailout:"));
            }),
          mavenProject("multi-project-build",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>sub-a</module>
                        <module>sub-b</module>
                    </modules>
                </project>
                """
            ),
            mavenProject("sub-a",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-a</artifactId>
                  </project>
                  """
              )
            ),
            mavenProject("sub-b",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-b</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    // ---------------------------------------------------------------------
    // Gradle multi-module
    // ---------------------------------------------------------------------

    @Test
    void gradleBuildFileChange_cascadesDownstream() {
        // module-a depends on module-b; a change in module-b/build.gradle should
        // flag both module-b ("build-file-changed") and module-a ("module-dep-of-affected").
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/module-b/build.gradle", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows)
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-b");
                            assertThat(r.getReason()).isEqualTo("build-file-changed");
                        })
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-a");
                            assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                        });
            }),
          mavenProject("multi-project-build",
            settingsGradle(
              //language=groovy
              """
                include 'module-a'
                include 'module-b'
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  dependencies {
                      implementation project(':module-b')
                  }
                  """
              )
            ),
            mavenProject("module-b",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }

    @Test
    void gradleSourceChange_cascadesToTransitiveDependents() {
        // module-a depends on module-b. A Java source change in module-b should
        // flag module-b ("source-changed") AND module-a ("module-dep-of-affected")
        // because module-a's tests exercise module-b's code. This is the core
        // dogfood scenario: change source in B -> run tests in A and B.
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/module-b/src/main/java/foo/Adder.java", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows)
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-b");
                            assertThat(r.getReason()).isEqualTo("source-changed");
                        })
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-a");
                            assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                        });
            }),
          mavenProject("multi-project-build",
            settingsGradle(
              //language=groovy
              """
                include 'module-a'
                include 'module-b'
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  dependencies {
                      implementation project(':module-b')
                  }
                  """
              )
            ),
            mavenProject("module-b",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }

    @Test
    void mavenSourceChange_cascadesToTransitiveDependents() {
        // sub-b depends on sub-a. A Java source change in sub-a should flag
        // sub-a ("source-changed") AND sub-b ("module-dep-of-affected").
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/sub-a/src/main/java/foo/Adder.java", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows)
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/sub-a");
                            assertThat(r.getReason()).isEqualTo("source-changed");
                        })
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/sub-b");
                            assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                        });
            }),
          mavenProject("multi-project-build",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>sub-a</module>
                        <module>sub-b</module>
                    </modules>
                </project>
                """
            ),
            mavenProject("sub-a",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-a</artifactId>
                  </project>
                  """
              )
            ),
            mavenProject("sub-b",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                      </parent>
                      <artifactId>sub-b</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.example</groupId>
                              <artifactId>sub-a</artifactId>
                              <version>1</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    // ---------------------------------------------------------------------
    // Repo-root no-op files
    // ---------------------------------------------------------------------

    @Test
    void rootReadmeChange_emitsNothing() {
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/README.md", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows).isEmpty();
            }),
          mavenProject("multi-project-build",
            settingsGradle(
              //language=groovy
              """
                include 'module-a'
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }

    @Test
    void rootGitignoreChange_emitsNothing() {
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/.gitignore", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows).isEmpty();
            }),
          mavenProject("multi-project-build",
            settingsGradle(
              //language=groovy
              """
                include 'module-a'
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }

    // ---------------------------------------------------------------------
    // Repo-root fallback files
    // ---------------------------------------------------------------------

    @Test
    void rootSettingsGradleKtsChange_triggersFullRepoFallback() {
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/settings.gradle.kts", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                List<String> modules = rows.stream()
                        .map(AffectedModulesDataTable.Row::getModule)
                        .collect(Collectors.toList());
                assertThat(modules)
                        .contains("multi-project-build", "multi-project-build/module-a");
                assertThat(rows).allSatisfy(r ->
                        assertThat(r.getReason()).isEqualTo("repo-root-bailout:settings.gradle.kts"));
            }),
          mavenProject("multi-project-build",
            settingsGradleKts(
              //language=kotlin
              """
                include("module-a")
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }

    /**
     * Build a {@link GradleProject} marker whose {@code implementation} configuration
     * requests project dependencies on each of {@code projectPaths} (e.g.
     * {@code ":module-b"}). The path of the marker itself is {@code myPath}.
     */
    private static GradleProject gradleProjectMarker(String myPath, String... projectPaths) {
        List<Dependency> requested = new ArrayList<>();
        for (String target : projectPaths) {
            Map<String, String> attrs = new HashMap<>();
            attrs.put(ProjectAttribute.key(), target);
            Dependency d = new Dependency(
                    new GroupArtifactVersion("org.example", extractName(target), null),
                    null, null, "implementation",
                    Collections.emptyList(), null, attrs);
            requested.add(d);
        }
        GradleDependencyConfiguration impl = new GradleDependencyConfiguration(
                "implementation", null,
                true, false, false, true,
                Collections.emptyList(), requested, Collections.emptyList(),
                null, null);
        Map<String, GradleDependencyConfiguration> configs = new HashMap<>();
        configs.put("implementation", impl);
        return GradleProject.builder()
                .group("org.example")
                .name(extractName(myPath))
                .version("1.0")
                .path(myPath)
                .nameToConfiguration(configs)
                .build();
    }

    private static String extractName(String gradlePath) {
        int idx = gradlePath.lastIndexOf(':');
        return idx < 0 ? gradlePath : gradlePath.substring(idx + 1);
    }

    @Test
    void kotlinDslSourceChangeCascadesViaGradleProjectMarker() {
        // Kotlin DSL parity for {@link #gradleSourceChange_cascadesToTransitiveDependents()}:
        // module-a depends on module-b via `implementation(project(":module-b"))`.
        // The build scripts are `build.gradle.kts` files that do NOT have Gradle DSL
        // type attribution (so the MethodMatcher path would miss the edge), but they
        // DO carry a GradleProject marker. The recipe must pick up the inter-project
        // dependency edge via the marker's configurations/ProjectAttribute and cascade
        // the source change in module-b to module-a.
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/module-b/src/main/kotlin/foo/Adder.kt", "MODIFIED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                assertThat(rows)
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-b");
                            assertThat(r.getReason()).isEqualTo("source-changed");
                        })
                        .anySatisfy(r -> {
                            assertThat(r.getModule()).isEqualTo("multi-project-build/module-a");
                            assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                        });
            }),
          mavenProject("multi-project-build",
            settingsGradleKts(
              //language=kotlin
              """
                include("module-a")
                include("module-b")
                """
            ),
            mavenProject("module-a",
              buildGradleKts(
                //language=kotlin
                """
                  plugins { `java-library` }
                  dependencies {
                      implementation(project(":module-b"))
                  }
                  """,
                s -> s.markers(gradleProjectMarker(":module-a", ":module-b"))
              )
            ),
            mavenProject("module-b",
              buildGradleKts(
                //language=kotlin
                """
                  plugins { `java-library` }
                  """,
                s -> s.markers(gradleProjectMarker(":module-b"))
              )
            )
          )
        );
    }

    @Test
    void unknownRootFile_triggersFullRepoBailoutWithPathReason() {
        rewriteRun(
          spec -> spec.recipe(new ClassifyAffectedModules())
            .executionContext(ctxWithChanges(
              "multi-project-build/random-file.xyz", "ADDED"
            ))
            .afterRecipe(run -> {
                List<AffectedModulesDataTable.Row> rows = readAffected(run.getDataTableStore());
                List<String> modules = rows.stream()
                        .map(AffectedModulesDataTable.Row::getModule)
                        .collect(Collectors.toList());
                assertThat(modules)
                        .contains("multi-project-build", "multi-project-build/module-a");
                assertThat(rows).allSatisfy(r ->
                        assertThat(r.getReason()).isEqualTo("repo-root-bailout:random-file.xyz"));
            }),
          mavenProject("multi-project-build",
            settingsGradle(
              //language=groovy
              """
                include 'module-a'
                """
            ),
            mavenProject("module-a",
              buildGradle(
                //language=groovy
                """
                  plugins { id 'java' }
                  """
              )
            )
          )
        );
    }
}
