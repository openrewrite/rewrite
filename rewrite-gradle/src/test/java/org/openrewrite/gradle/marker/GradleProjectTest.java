/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.marker;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;

class GradleProjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .validateRecipeSerialization(false);
    }

    @Test
    void noopUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-java", "8.56.0"),
            "implementation",
            (original, updated) -> assertThat(updated).isSameAs(original)
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("org.openrewrite:rewrite-java:8.56.0")
              }
              """
          )
        );
    }

    @Test
    void multiProject() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-java", "8.57.0"),
            "implementation",
            (original, updated) -> {
                // mostly interested that a ProjectDependency does not cause an exception
            }
          )),
          mavenProject("root",
            settingsGradle(
              """
                include("a")
                include("b")
                """,
              SourceSpec::skip
            ),
            mavenProject("a",
              buildGradle(
                """
                  plugins {
                      id("java")
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      implementation("org.openrewrite:rewrite-java:8.56.0")
                  }
                  """,
                SourceSpec::skip
              )
            ),
            mavenProject("b",
              buildGradle(
                """
                  plugins {
                      id("java")
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      implementation(project(":a"))
                      implementation("org.openrewrite:rewrite-java:8.56.0")
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void plusVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-java", "8.57.0"),
            "implementation",
            (original, updated) -> {
                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();
                List<GradleDependencyConfiguration> updatedConfigurations = ListUtils.concat(implementation, updated.configurationsExtendingFrom(implementation, true));
                for (GradleDependencyConfiguration updatedConfiguration : updatedConfigurations) {
                    Dependency requested = updatedConfiguration.findRequestedDependency("org.openrewrite", "rewrite-java");
                    assertThat(requested).isNotNull()
                      .extracting(Dependency::getVersion)
                      .as(updatedConfiguration.getName() + " expected to have requested version upgrade")
                      .isEqualTo("8.57.0");
                    if (updatedConfiguration.isCanBeResolved()) {
                        ResolvedDependency resolved = updatedConfiguration.findResolvedDependency("org.openrewrite", "rewrite-java");
                        assertThat(resolved).isNotNull()
                          .extracting(ResolvedDependency::getVersion)
                          .as(updatedConfiguration.getName() + " expected to have resolved version upgrade")
                          .isEqualTo("8.57.0");
                    }
                }
            }
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("org.openrewrite:rewrite-java:8.56.+")
              }
              """
          )
        );
    }

    @Test
    void simpleUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-java", "8.57.0"),
            "implementation",
            (original, updated) -> {
                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();
                List<GradleDependencyConfiguration> updatedConfigurations = ListUtils.concat(implementation, updated.configurationsExtendingFrom(implementation, true));
                for (GradleDependencyConfiguration updatedConfiguration : updatedConfigurations) {
                    Dependency requested = updatedConfiguration.findRequestedDependency("org.openrewrite", "rewrite-java");
                    assertThat(requested).isNotNull()
                      .extracting(Dependency::getVersion)
                      .as(updatedConfiguration.getName() + " expected to have requested version upgrade")
                      .isEqualTo("8.57.0");
                    if (updatedConfiguration.isCanBeResolved()) {
                        ResolvedDependency resolved = updatedConfiguration.findResolvedDependency("org.openrewrite", "rewrite-java");
                        assertThat(resolved).isNotNull()
                          .extracting(ResolvedDependency::getVersion)
                          .as(updatedConfiguration.getName() + " expected to have resolved version upgrade")
                          .isEqualTo("8.57.0");
                    }
                }

            }
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("org.openrewrite:rewrite-java:8.56.0")
              }
              """
          )
        );
    }

    @Test
    void bomUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-bom", "8.57.0"),
            "implementation",
            (original, updated) -> {
                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();
                List<GradleDependencyConfiguration> updatedConfigurations = ListUtils.concat(implementation, updated.configurationsExtendingFrom(implementation, true));
                for (GradleDependencyConfiguration updatedConfiguration : updatedConfigurations) {
                    Dependency requested = updatedConfiguration.findRequestedDependency("org.openrewrite", "rewrite-java");
                    assertThat(requested).isNotNull()
                      .extracting(Dependency::getVersion)
                      .as(updatedConfiguration.getName() + " expected to have requested version upgrade")
                      .isNull();
                    if (updatedConfiguration.isCanBeResolved()) {
                        ResolvedDependency resolved = updatedConfiguration.findResolvedDependency("org.openrewrite", "rewrite-java");
                        assertThat(resolved).isNotNull()
                          .extracting(ResolvedDependency::getVersion)
                          .as(updatedConfiguration.getName() + " expected to have resolved version upgrade")
                          .isEqualTo("8.57.0");
                    }
                }

            }
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation(platform("org.openrewrite:rewrite-bom:8.56.0"))
                  implementation("org.openrewrite:rewrite-java")
              }
              """
          )
        );
    }

    @Test
    void removesDefunctTransitives() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyInMarker(
            new GroupArtifactVersion("io.vertx", "vertx-core", "5.0.1"),
            "implementation",
            (original, updated) -> {
                assertThat(updated).isNotSameAs(original);

                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();

                Dependency requested = implementation.findRequestedDependency("io.vertx", "vertx-core");
                assertThat(requested).isNotNull()
                  .extracting(Dependency::getVersion)
                  .isEqualTo("5.0.1");

                List<GradleDependencyConfiguration> resolvableConfigurations = ListUtils.concat(implementation, updated.configurationsExtendingFrom(implementation, true));
                for (GradleDependencyConfiguration config : resolvableConfigurations) {
                    if (config.isCanBeResolved()) {
                        ResolvedDependency vertxCore = config.findResolvedDependency("io.vertx", "vertx-core");
                        if (vertxCore != null) {
                            assertThat(vertxCore.getVersion())
                              .as(config.getName() + " should have vertx-core 5.0.1")
                              .isEqualTo("5.0.1");
                        }

                        // Check that netty-codec is NOT listed amongst the dependencies as it is not a dependency of vertx-core 5.0.1
                        assertThat(config.getResolved())
                          .as(config.getName() + " should NOT contain netty-codec after upgrade to vertx-core 5.0.1")
                          .noneMatch(dep -> "io.netty".equals(dep.getGroupId()) && "netty-codec".equals(dep.getArtifactId()));
                    }
                }
            }
          )),
          buildGradle(
            """
              plugins {
                  id("java-library")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("io.vertx:vertx-core:3.9.8")
              }
              """
          )
        );
    }

    @Test
    void removeDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency(
            List.of(new GroupArtifact("org.openrewrite", "rewrite-core")),
            (original, updated) -> {
                assertThat(updated).isNotSameAs(original);

                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();

                Dependency rewriteCore = implementation.findRequestedDependency("org.openrewrite", "rewrite-core");
                assertThat(rewriteCore)
                  .as("rewrite-core should have been removed")
                  .isNull();

            }
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("org.openrewrite:rewrite-core:8.56.0")
                  implementation("org.openrewrite:rewrite-java:8.56.0")
              }
              """
          )
        );
    }

    @Test
    void changeConstraint() {
        rewriteRun(
          spec -> spec.recipe(new ChangeConstraint(
            Map.of("implementation", List.of(new GroupArtifactVersion("com.fasterxml.jackson.core", "jackson-databind", "2.19.2"))),
            (original, updated) -> {
                GradleDependencyConfiguration implementation = updated.getConfiguration("implementation");
                assertThat(implementation).isNotNull();

                Dependency rewriteCore = implementation.findRequestedDependency("org.openrewrite", "rewrite-core");
                assertThat(rewriteCore).isNotNull()
                  .extracting(Dependency::getVersion)
                  .as("rewrite-core version should not have changed")
                  .isEqualTo("8.56.0");

                // Verify the constraint was updated
                List<GradleDependencyConstraint> constraints = implementation.getConstraints();
                assertThat(constraints).isNotNull();

                GradleDependencyConstraint jacksonConstraint = constraints.stream()
                  .filter(c -> "com.fasterxml.jackson.core".equals(c.getGroupId()) &&
                               "jackson-databind".equals(c.getArtifactId()))
                  .findFirst()
                  .orElse(null);

                assertThat(jacksonConstraint).isNotNull()
                  .extracting(GradleDependencyConstraint::getRequiredVersion)
                  .isEqualTo("2.19.2");

                // Verify the runtimeClasspath has the updated resolved version
                GradleDependencyConfiguration runtimeClasspath = updated.getConfiguration("runtimeClasspath");
                assertThat(runtimeClasspath).isNotNull();
                ResolvedDependency resolvedJackson = runtimeClasspath.findResolvedDependency("com.fasterxml.jackson.core", "jackson-databind");
                assertThat(resolvedJackson).isNotNull()
                  .extracting(ResolvedDependency::getVersion)
                  .as("jackson-databind should be resolved to the constrained version")
                  .isEqualTo("2.19.2");
            }
          )),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
                  }
                  implementation("org.openrewrite:rewrite-core:8.56.0")
              }
              """
          )
        );
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class UpgradeDependencyInMarker extends Recipe {

    GroupArtifactVersion newGav;
    String configuration;
    BiConsumer<GradleProject, GradleProject> testAssertion;

    @Override
    public String getDisplayName() {
        return "Upgrade a version within the GradleProject marker";
    }

    @Override
    public String getDescription() {
        return "Upgrade a version within the GradleProject marker. Makes no changes to the source file itself";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public Tree visit(Tree tree, ExecutionContext ctx) {
                GradleProject original = tree.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> fail("Missing GradleProject"));
                GradleProject updated = original.upgradeDirectDependencyVersion(configuration, newGav, ctx);
                testAssertion.accept(original, updated);
                return tree;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class RemoveDependency extends Recipe {

    List<GroupArtifact> gas;

    @Nullable
    String configuration;

    BiConsumer<GradleProject, GradleProject> testAssertion;

    public RemoveDependency(List<GroupArtifact> gas, BiConsumer<GradleProject, GradleProject> testAssertion) {
        this.gas = gas;
        this.configuration = null;
        this.testAssertion = testAssertion;
    }

    @Override
    public String getDisplayName() {
        return "Remove a dependency within the GradleProject marker";
    }

    @Override
    public String getDescription() {
        return "Remove a dependency within the GradleProject marker. Makes no changes to the source file itself";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public Tree visit(Tree tree, ExecutionContext ctx) {
                GradleProject original = tree.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> fail("Missing GradleProject"));
                GradleProject updated = original.removeDirectDependencies(gas, ctx);
                testAssertion.accept(original, updated);
                return tree;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class ChangeConstraint extends Recipe {

    Map<String, List<GroupArtifactVersion>> configToConstraint;
    BiConsumer<GradleProject, GradleProject> testAssertion;

    @Override
    public String getDisplayName() {
        return "Remove a dependency within the GradleProject marker";
    }

    @Override
    public String getDescription() {
        return "Remove a dependency within the GradleProject marker. Makes no changes to the source file itself";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public Tree visit(Tree tree, ExecutionContext ctx) {
                GradleProject original = tree.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> fail("Missing GradleProject"));
                GradleProject updated = original.addOrUpdateConstraints(configToConstraint, ctx);
                testAssertion.accept(original, updated);
                return tree;
            }
        };
    }
}
