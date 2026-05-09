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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.InMemoryDataTableStore;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.table.AffectedModulesDataTable;
import org.openrewrite.table.TestPlanDataTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.test.SourceSpecs.text;

class SelectTestsInAffectedModulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
                .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
                .recipe(new SelectTestsInAffectedModules());
    }

    /**
     * Seed the shared {@link InMemoryDataTableStore} with {@link AffectedModulesDataTable}
     * rows. The test-selection recipe reads these in its {@code getInitialValue}.
     */
    private static ExecutionContext ctxWithAffected(String... modules) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable table = new AffectedModulesDataTable(Recipe.noop());
        for (String module : modules) {
            store.insertRow(table, ctx, new AffectedModulesDataTable.Row(module, "module-dep-changed", "", ""));
        }
        return ctx;
    }

    @Test
    void happyPathJavaTestInAffectedModule() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getModule()).isEqualTo("foo");
                                    assertThat(r.getTestClass()).isEqualTo("com.example.FooTest");
                                    assertThat(r.getTestMethod()).isEmpty();
                                    assertThat(r.getReason()).isEqualTo("module-dep-changed");
                                    assertThat(r.getLanguage()).isEqualTo("java");
                                    // No build files present — default to gradle
                                    assertThat(r.getRunner()).isEqualTo("gradle");
                                })),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    @Test
    void emptyWhenNoAffectedModules() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected())
                        .afterRecipe(run -> {
                            // No AffectedModules rows = no TestPlan rows emitted.
                            boolean hasTestPlan = run.getDataTableStore().getDataTables().stream()
                                    .anyMatch(dt -> TestPlanDataTable.class.getName().equals(dt.getName()));
                            assertThat(hasTestPlan).isFalse();
                        }),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    @Test
    void unaffectedModulesAreIgnored() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .dataTable(TestPlanDataTable.Row.class, rows -> {
                            // Only "foo" should produce a row; "bar" is not in the affected set.
                            assertThat(rows).singleElement().satisfies(r ->
                                    assertThat(r.getModule()).isEqualTo("foo"));
                        }),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example.foo;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                ),
                mavenProject("bar",
                        srcTestJava(
                                java(
                                        """
                                          package com.example.bar;
                                          import org.junit.jupiter.api.Test;
                                          public class BarTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    // Note: a JUnit 4 (`@org.junit.Test`) happy-path test would require JUnit 4 on
    // the rewrite-java test classpath. The AnnotationMatcher lookup requires a
    // resolvable JavaType for the annotation, and rewrite-java only provides
    // junit-jupiter-api. The recipe still matches `@org.junit.Test` in production
    // LSTs — see the TEST_ANNOTATION_PATTERNS constant. Cross-lane integration
    // tests exercise this path.

    @Test
    void runnerInferredAsMvnWhenPomXmlPresent() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getModule()).isEqualTo("foo");
                                    assertThat(r.getRunner()).isEqualTo("mvn");
                                })),
                mavenProject("foo",
                        text(
                                """
                                  <project/>
                                  """,
                                spec -> spec.path("pom.xml")
                        ),
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    @Test
    void classWithoutTestAnnotationIsIgnored() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .afterRecipe(run -> {
                            boolean hasTestPlan = run.getDataTableStore().getDataTables().stream()
                                    .anyMatch(dt -> TestPlanDataTable.class.getName().equals(dt.getName()));
                            assertThat(hasTestPlan).isFalse();
                        }),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          public class PlainClass {
                                              void notATest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * The requirement "Kotlin test file using Java @Test mechanism → discovered"
     * is exercised here at the language-inference layer. The full Kotlin parse
     * path lives in rewrite-kotlin (out of this module's dependency scope), so
     * we assert that the extension-based language classifier returns "kotlin"
     * for {@code .kt} paths — which is the sole piece of Kotlin-specific logic
     * in the selector.
     */
    @Test
    void languageInferenceForKotlinExtension() {
        assertThat(SelectTestsInAffectedModules.languageFromPath("foo/src/test/kotlin/Bar.kt"))
                .isEqualTo("kotlin");
        assertThat(SelectTestsInAffectedModules.languageFromPath("foo/src/test/groovy/Bar.groovy"))
                .isEqualTo("groovy");
        assertThat(SelectTestsInAffectedModules.languageFromPath("foo/src/test/java/Bar.java"))
                .isEqualTo("java");
        assertThat(SelectTestsInAffectedModules.languageFromPath("foo/README.md"))
                .isNull();
    }

    @Test
    void moduleForBuildFileInference() {
        assertThat(SelectTestsInAffectedModules.moduleForBuildFile("foo/pom.xml")).isEqualTo("foo");
        assertThat(SelectTestsInAffectedModules.moduleForBuildFile("foo/bar/build.gradle")).isEqualTo("foo/bar");
        assertThat(SelectTestsInAffectedModules.moduleForBuildFile("foo/build.gradle.kts")).isEqualTo("foo");
        assertThat(SelectTestsInAffectedModules.moduleForBuildFile("pom.xml")).isEqualTo("");
        assertThat(SelectTestsInAffectedModules.moduleForBuildFile("foo/src/main/java/Foo.java")).isNull();
    }

    /**
     * Bug #2, part 1: {@code @kotlin.test.Test} should be treated the same as the
     * JUnit/TestNG annotations. rewrite-kotlin is not a test-time dependency of
     * rewrite-java (that would be a cross-module dependency), so we exercise the
     * annotation matcher via a Java source file plus a stubbed {@code kotlin.test}
     * package. The {@code language} column is inferred from the file extension and
     * is asserted in {@link #languageInferenceForKotlinExtension()}; here we only
     * care that the matcher fires for the annotation FQN.
     */
    @Test
    void kotlinTestAnnotationIsDiscovered() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .parser(JavaParser.fromJavaVersion().dependsOn(
                                """
                                  package kotlin.test;
                                  import java.lang.annotation.*;
                                  @Retention(RetentionPolicy.RUNTIME)
                                  @Target({ElementType.METHOD, ElementType.TYPE})
                                  public @interface Test {}
                                  """))
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getModule()).isEqualTo("foo");
                                    assertThat(r.getTestClass()).isEqualTo("com.example.KotlinStyleTest");
                                })),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import kotlin.test.Test;
                                          public class KotlinStyleTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * Bug #2, part 2: a class extending {@code spock.lang.Specification} is a
     * Spock test whether or not any of its methods carry a {@code @Test}-style
     * annotation. We stub a minimal {@code Specification} base class so the Java
     * parser can produce a type-attributed {@code J.ClassDeclaration} whose
     * supertype chain the recipe can inspect via {@link
     * org.openrewrite.java.tree.TypeUtils#isAssignableTo}.
     */
    // -----------------------------------------------------------------------
    // Phase 2: reachability integration
    // -----------------------------------------------------------------------

    /**
     * Seeds both an affected-module row (reason `module-dep-of-affected`) and a
     * reachability row that marks {@code reachableClass} as transitively reaching
     * a changed symbol. The downstream selector should include that class and
     * tag the row with {@code reachable-from-changed}.
     */
    private static ExecutionContext ctxWithModuleDepAndReachable(String module, String reachableClass) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);

        AffectedModulesDataTable mods = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(mods, ctx,
                new AffectedModulesDataTable.Row(module, "module-dep-of-affected", "rewrite-foo/src/main/java/Foo.java", "rewrite-foo"));

        ReachabilityFixtureTable reachTable = new ReachabilityFixtureTable(Recipe.noop());
        store.insertRow(reachTable, ctx,
                new ReachabilityFixtureTable.Row(reachableClass, "", reachableClass, "", 0));
        return ctx;
    }

    private static ExecutionContext ctxWithModuleDepAndBailout(String module) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable mods = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(mods, ctx,
                new AffectedModulesDataTable.Row(module, "module-dep-of-affected", "rewrite-foo/src/main/java/Foo.java", "rewrite-foo"));

        BailoutFixtureTable bailoutTable = new BailoutFixtureTable(Recipe.noop());
        store.insertRow(bailoutTable, ctx,
                new BailoutFixtureTable.Row("reachability-exceeded-iterations", "iteration=50"));
        return ctx;
    }

    @Test
    void phase2_reachabilityDataFiltersUnreachableTests() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithModuleDepAndReachable("foo", "com.example.ReachableTest"))
                        .dataTable(TestPlanDataTable.Row.class, rows -> {
                            // Only ReachableTest should be emitted — UnreachableTest is pruned.
                            // Reason is preserved as-is (the upstream module-level signal);
                            // reachability is just a filter, no longer a relabeling step.
                            assertThat(rows).singleElement().satisfies(r -> {
                                assertThat(r.getTestClass()).isEqualTo("com.example.ReachableTest");
                                assertThat(r.getReason()).isEqualTo("module-dep-of-affected");
                            });
                        }),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class ReachableTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                ),
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class UnreachableTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    @Test
    void phase2_bailoutFallsBackToAllTestsInModule() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithModuleDepAndBailout("foo"))
                        .dataTable(TestPlanDataTable.Row.class, rows -> {
                            // With a bailout row present, Phase 2 falls back to Phase 1 —
                            // both tests should be selected, both retaining the original
                            // module-dep-of-affected reason.
                            assertThat(rows).hasSize(2);
                            assertThat(rows).allSatisfy(r ->
                                    assertThat(r.getReason()).isEqualTo("module-dep-of-affected"));
                        }),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class ReachableTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                ),
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class OtherTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * When the module is flagged as {@code source-changed} (not module-dep), every
     * test in the module is still selected regardless of reachability data. Phase 2
     * narrowing only applies to {@code module-dep-of-affected} rows.
     */
    /**
     * Reachability is the foundation for selection in <em>every</em> affected
     * module — including ones with their own source edits. A large module that
     * happened to receive a source change should not drop every one of its
     * tests on the runner; we want only the tests that the BFS proves reach
     * the changed code.
     *
     * <p>Trade-off: tests that route through reflection, DI, annotation
     * processors, or Kotlin metadata-driven dispatch can be silently dropped.
     * That's the explicit current bargain — framework-aware adapters are a
     * subsequent phase.</p>
     */
    @Test
    void phase2_sourceChangedFiltersByReachability() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable mods = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(mods, ctx, new AffectedModulesDataTable.Row("foo", "source-changed", "foo/src/main/java/Foo.java", ""));

        // Reachability seeds only ReachableTest. With reachability now applied
        // to source-changed modules too, OtherTest should be filtered out.
        ReachabilityFixtureTable reachTable = new ReachabilityFixtureTable(Recipe.noop());
        store.insertRow(reachTable, ctx,
                new ReachabilityFixtureTable.Row("com.example.ReachableTest", "", "com.example.Something", "", 1));

        rewriteRun(
                spec -> spec
                        .executionContext(ctx)
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getTestClass()).isEqualTo("com.example.ReachableTest");
                                    // Reason still describes the module-level signal —
                                    // we don't relabel as "reachable-from-changed" now
                                    // that the path column already shows the chain.
                                    assertThat(r.getReason()).isEqualTo("source-changed");
                                })),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class ReachableTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                ),
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class OtherTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * Reachability still does not gate {@code build-file-changed} modules:
     * editing a pom.xml / build.gradle has no Java-symbol diff to seed the
     * BFS with, so we keep the module's tests in conservatively. Anything
     * else would silently drop every test on a dependency-version bump.
     */
    @Test
    void buildFileChangedSkipsReachabilityFilter() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable mods = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(mods, ctx, new AffectedModulesDataTable.Row("foo", "build-file-changed", "foo/pom.xml", ""));
        // Reachability rows from some other module — should be ignored here.
        ReachabilityFixtureTable reachTable = new ReachabilityFixtureTable(Recipe.noop());
        store.insertRow(reachTable, ctx,
                new ReachabilityFixtureTable.Row("com.example.UnrelatedTest", "", "com.example.X", "", 1));

        rewriteRun(
                spec -> spec
                        .executionContext(ctx)
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).hasSize(2).allSatisfy(r ->
                                        assertThat(r.getReason()).isEqualTo("build-file-changed"))),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class ReachableTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                ),
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class OtherTest {
                                              @Test
                                              void t() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    @Test
    void spockSpecificationIsDiscovered() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithAffected("foo"))
                        .parser(JavaParser.fromJavaVersion().dependsOn(
                                """
                                  package spock.lang;
                                  public class Specification {}
                                  """))
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getModule()).isEqualTo("foo");
                                    assertThat(r.getTestClass()).isEqualTo("com.example.GreeterSpec");
                                    // No annotation matched — selection was via supertype.
                                    assertThat(r.getReason()).isEqualTo("module-dep-changed");
                                })),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import spock.lang.Specification;
                                          public class GreeterSpec extends Specification {
                                              void greeting() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * A module can appear in AffectedModulesDataTable with multiple reasons — for
     * example, {@code source-changed} because its own source was edited AND
     * {@code module-dep-of-affected} because the cascade added it as a transitive
     * dependent of another edited module. The priority merge keeps the most
     * informative reason (source-changed) so the user sees the strongest signal
     * for why the module was included.
     *
     * <p>Note: under the current "reachability is the foundation" rule, both
     * reasons would have produced the same set of selected tests anyway —
     * reachability filters in either case. The priority merge therefore affects
     * the {@code reason} label, not the row count.</p>
     */
    @Test
    void sourceChangedOverridesModuleDepOfAffectedForSameModule() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable table = new AffectedModulesDataTable(Recipe.noop());
        // Emit module-dep-of-affected FIRST, then source-changed. The put-last-wins
        // bug would have left source-changed as the winning reason purely by
        // iteration order; the priority merge ensures it wins on merit.
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "module-dep-of-affected", "bar/src/main/java/Bar.java", "bar"));
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "source-changed", "foo/src/main/java/Foo.java", ""));

        rewriteRun(
                spec -> spec
                        .executionContext(ctx)
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r -> {
                                    assertThat(r.getModule()).isEqualTo("foo");
                                    assertThat(r.getReason())
                                            .as("source-changed must win over module-dep-of-affected for the same module")
                                            .isEqualTo("source-changed");
                                })),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }

    /**
     * Reverse order of {@link #sourceChangedOverridesModuleDepOfAffectedForSameModule}:
     * same conclusion (source-changed wins) regardless of insertion order.
     */
    @Test
    void sourceChangedWinsRegardlessOfInsertionOrder() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable table = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "source-changed", "foo/src/main/java/Foo.java", ""));
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "module-dep-of-affected", "bar/src/main/java/Bar.java", "bar"));

        rewriteRun(
                spec -> spec
                        .executionContext(ctx)
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).singleElement().satisfies(r ->
                                        assertThat(r.getReason()).isEqualTo("source-changed"))),
                mavenProject("foo",
                        srcTestJava(
                                java(
                                        """
                                          package com.example;
                                          import org.junit.jupiter.api.Test;
                                          public class FooTest {
                                              @Test
                                              void someTest() {}
                                          }
                                          """
                                )
                        )
                )
        );
    }
}
