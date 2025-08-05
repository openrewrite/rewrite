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

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

public class GradleProjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .validateRecipeSerialization(false);
    }

    @Test
    void noopUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeMarker(
            new GroupArtifactVersion("org.openrewrite", "rewrite-java", "8.56.0"),
            "implementation",
            (original, updated) -> assertThat(updated).isSameAs(original)
          )),
          buildGradle("""
            plugins {
                id("java")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation("org.openrewrite:rewrite-java:8.56.0")
            }
            """)
        );
    }

    @Test
    void simpleUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeMarker(
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
                    if(updatedConfiguration.isCanBeResolved()) {
                        ResolvedDependency resolved = updatedConfiguration.findResolvedDependency("org.openrewrite", "rewrite-java");
                        assertThat(resolved).isNotNull()
                          .extracting(ResolvedDependency::getVersion)
                          .as(updatedConfiguration.getName() + " expected to have resolved version upgrade")
                          .isEqualTo("8.57.0");
                    }
                }

            }
          )),
          buildGradle("""
            plugins {
                id("java")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation("org.openrewrite:rewrite-java:8.56.0")
            }
            """)
        );
    }

    @Test
    void removesDefunctTransitives() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeMarker(
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
                          .noneMatch(dep -> dep.getGroupId().equals("io.netty") && dep.getArtifactId().equals("netty-codec"));
                    }
                }
            }
          )),
          buildGradle("""
            plugins {
                id("java-library")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation("io.vertx:vertx-core:3.9.8")
            }
            """)
        );
    }

    @Test
    void removeDependency() {
        rewriteRun(
          spec -> spec.recipe(new Remove(
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
          buildGradle("""
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
            """)
        );
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class UpgradeMarker extends Recipe {

    List<GroupArtifactVersion> newGavs;
    @Nullable
    String configuration;
    BiConsumer<GradleProject, GradleProject> testAssertion;

    public UpgradeMarker(GroupArtifactVersion newGav, String configuration, BiConsumer<GradleProject, GradleProject> testAssertion) {
        this.newGavs = List.of(newGav);
        this.configuration = configuration;
        this.testAssertion = testAssertion;
    }

    public UpgradeMarker(List<GroupArtifactVersion> newGavs, BiConsumer<GradleProject, GradleProject> testAssertion) {
        this.newGavs = newGavs;
        this.configuration = null;
        this.testAssertion = testAssertion;
    }

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
            @SneakyThrows
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                GradleProject original = tree.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> fail("Missing GradleProject"));
                GradleProject updated;
                if (configuration == null) {
                    updated = original.upgradeDependencyVersions(newGavs, ctx);
                } else {
                    updated = original.upgradeDependencyVersion(configuration, newGavs.getFirst(), ctx);
                }
                testAssertion.accept(original, updated);
                return tree;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class Remove extends Recipe {

    List<GroupArtifact> gas;
    @Nullable
    String configuration;
    BiConsumer<GradleProject, GradleProject> testAssertion;

    public Remove(GroupArtifact gas, String configuration, BiConsumer<GradleProject, GradleProject> testAssertion) {
        this.gas = List.of(gas);
        this.configuration = configuration;
        this.testAssertion = testAssertion;
    }

    public Remove(List<GroupArtifact> gas, BiConsumer<GradleProject, GradleProject> testAssertion) {
        this.gas = gas;
        this.configuration = null;
        this.testAssertion = testAssertion;
    }

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
            @SneakyThrows
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                GradleProject original = tree.getMarkers().findFirst(GradleProject.class).orElseThrow(() -> fail("Missing GradleProject"));
                GradleProject updated = original.removeDirectDependencies(gas, ctx);
                testAssertion.accept(original, updated);
                return tree;
            }
        };
    }
}
