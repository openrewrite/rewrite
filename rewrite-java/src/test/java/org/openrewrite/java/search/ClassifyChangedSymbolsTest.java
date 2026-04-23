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
import org.openrewrite.InMemoryDataTableStore;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.table.ChangedFilesDataTable;
import org.openrewrite.table.ChangedSymbolsDataTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

/**
 * Verifies that {@link ClassifyChangedSymbols} reads the changed-files table and
 * emits one row per declared class/method/field/constructor in each changed file.
 */
class ClassifyChangedSymbolsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ClassifyChangedSymbols());
    }

    private static ExecutionContext ctxWithChanges(String path, String changeType) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        DataTableExecutionContextView.view(ctx).setDataTableStore(store);
        ChangedFilesDataTable table = new ChangedFilesDataTable(Recipe.noop());
        store.insertRow(table, ctx, new ChangedFilesDataTable.Row(path, changeType));
        return ctx;
    }

    @Test
    void emitsClassMethodConstructorAndField() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithChanges("src/main/java/com/example/Foo.java", "MODIFIED"))
                        .dataTable(ChangedSymbolsDataTable.Row.class, rows -> {
                            assertThat(rows).extracting(ChangedSymbolsDataTable.Row::getClassName)
                                    .contains("com.example.Foo");

                            assertThat(rows).anySatisfy(r -> {
                                assertThat(r.getClassName()).isEqualTo("com.example.Foo");
                                assertThat(r.getMemberKind()).isEqualTo("CLASS");
                                assertThat(r.getMemberName()).isEmpty();
                                assertThat(r.getChangeType()).isEqualTo("MODIFIED");
                            });
                            assertThat(rows).anySatisfy(r -> {
                                assertThat(r.getMemberKind()).isEqualTo("METHOD");
                                assertThat(r.getMemberName()).isEqualTo("bar");
                            });
                            assertThat(rows).anySatisfy(r -> {
                                assertThat(r.getMemberKind()).isEqualTo("CONSTRUCTOR");
                                assertThat(r.getMemberName()).isEqualTo("<constructor>");
                            });
                            assertThat(rows).anySatisfy(r -> {
                                assertThat(r.getMemberKind()).isEqualTo("FIELD");
                                assertThat(r.getMemberName()).isEqualTo("baz");
                            });
                        }),
                //language=java
                java(
                        """
                        package com.example;
                        public class Foo {
                            int baz;
                            public Foo() {}
                            public void bar() {}
                        }
                        """,
                        spec -> spec.path("src/main/java/com/example/Foo.java")
                )
        );
    }

    @Test
    void ignoresUnchangedFiles() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithChanges("src/main/java/com/example/Other.java", "MODIFIED"))
                        .afterRecipe(run -> {
                            boolean hasSymbols = run.getDataTableStore().getDataTables().stream()
                                    .anyMatch(dt -> ChangedSymbolsDataTable.class.getName().equals(dt.getName()));
                            assertThat(hasSymbols).isFalse();
                        }),
                //language=java
                java(
                        """
                        package com.example;
                        public class Foo {
                            public void bar() {}
                        }
                        """,
                        spec -> spec.path("src/main/java/com/example/Foo.java")
                )
        );
    }

    @Test
    void carriesChangeTypeThrough() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithChanges("src/main/java/com/example/A.java", "ADDED"))
                        .dataTable(ChangedSymbolsDataTable.Row.class, rows ->
                                assertThat(rows).allSatisfy(r ->
                                        assertThat(r.getChangeType()).isEqualTo("ADDED"))),
                //language=java
                java(
                        """
                        package com.example;
                        public class A { public void m() {} }
                        """,
                        spec -> spec.path("src/main/java/com/example/A.java")
                )
        );
    }

    @Test
    void nestedClassesEmittedSeparately() {
        rewriteRun(
                spec -> spec
                        .executionContext(ctxWithChanges("src/main/java/com/example/Outer.java", "MODIFIED"))
                        .dataTable(ChangedSymbolsDataTable.Row.class, rows -> {
                            assertThat(rows).extracting(ChangedSymbolsDataTable.Row::getClassName)
                                    .contains("com.example.Outer", "com.example.Outer$Inner");
                        }),
                //language=java
                java(
                        """
                        package com.example;
                        public class Outer {
                            public static class Inner {
                                public void innerMethod() {}
                            }
                        }
                        """,
                        spec -> spec.path("src/main/java/com/example/Outer.java")
                )
        );
    }
}
