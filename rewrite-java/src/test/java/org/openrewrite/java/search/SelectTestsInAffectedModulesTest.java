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
            store.insertRow(table, ctx, new AffectedModulesDataTable.Row(module, "module-dep-changed"));
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
                new AffectedModulesDataTable.Row(module, "module-dep-of-affected"));

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
                new AffectedModulesDataTable.Row(module, "module-dep-of-affected"));

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
                            assertThat(rows).singleElement().satisfies(r -> {
                                assertThat(r.getTestClass()).isEqualTo("com.example.ReachableTest");
                                assertThat(r.getReason()).isEqualTo("reachable-from-changed");
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
    @Test
    void phase2_sourceChangedStillSelectsAllTests() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        AffectedModulesDataTable mods = new AffectedModulesDataTable(Recipe.noop());
        store.insertRow(mods, ctx, new AffectedModulesDataTable.Row("foo", "source-changed"));

        // Also seed a reachability row that names ONLY ReachableTest — but because
        // the reason is source-changed, both tests should still be emitted.
        ReachabilityFixtureTable reachTable = new ReachabilityFixtureTable(Recipe.noop());
        store.insertRow(reachTable, ctx,
                new ReachabilityFixtureTable.Row("com.example.ReachableTest", "", "com.example.Something", "", 1));

        rewriteRun(
                spec -> spec
                        .executionContext(ctx)
                        .dataTable(TestPlanDataTable.Row.class, rows ->
                                assertThat(rows).hasSize(2).allSatisfy(r ->
                                        assertThat(r.getReason()).isEqualTo("source-changed"))),
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
     * dependent of another edited module. In that case SelectTestsInAffectedModules
     * must keep the more conservative reason (source-changed) so every test in the
     * module is selected. If the put-last-wins map accidentally picked
     * module-dep-of-affected, the reachability filter would silently drop tests
     * that should have been included (empirically observed: 74 rewrite-kotlin tests
     * in the Phase 2 dogfood on openrewrite/rewrite, 2026-04-22).
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
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "module-dep-of-affected"));
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "source-changed"));

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
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "source-changed"));
        store.insertRow(table, ctx, new AffectedModulesDataTable.Row("foo", "module-dep-of-affected"));

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
