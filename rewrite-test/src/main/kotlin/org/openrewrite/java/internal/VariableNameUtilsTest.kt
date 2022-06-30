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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.internal.VariableNameUtils
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

interface VariableNameUtilsTest : RewriteTest {
    val parser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    @Test
    fun doNotAddPackagePrivateNameFromSuperClass() {
        // language=java
        val source = arrayOf("""
            package foo;
            public class Super {
                boolean pkgPrivate;
            }
        """.trimIndent(), """
            package bar;
            
            import foo.Super;
            
            class Test extends Super {
                boolean classBlock;
            }
        """.trimIndent())

        baseTest(source, "classBlock", "classBlock")
    }

    @Test
    fun staticImportedFieldNames() {
        // language=java
        val source = arrayOf("""
            import static java.nio.charset.StandardCharsets.UTF_8;
            import static java.util.Collections.emptyList;
            
            class Test {
                boolean classBlock;
            }
        """.trimIndent())

        baseTest(source, "classBlock", "classBlock, UTF_8, emptyList")
    }

    @Test
    fun allClassFieldsAreFound() {
        // language=java
        val source = arrayOf("""
            class Test {
                boolean classBlockA;
                void method() {
                    boolean methodBlock;
                }
                boolean classBlockB;
            }
        """.trimIndent())

        baseTest(source, "methodBlock", "methodBlock, classBlockA, classBlockB")
    }

    @Test
    fun findNamesAvailableFromBlock() {
        val sources = parser.parse(
            InMemoryExecutionContext(),
            """
                class Test {
                    boolean classFieldA;
                    void method (boolean methodParam) {
                        boolean methodBlockA;
                        for (int control = 0; control < 10; control++) {
                            boolean forBlock;
                            if (control == 5) {
                                boolean ifBlock;
                            }
                        }
                        boolean methodBlockB;
                    }
                    boolean classFieldB;
                }
            """.trimIndent()
        )
        val scope = "methodBlockA"
        val expected = setOf("classFieldA", "classFieldB", "methodBlockA", "methodBlockB", "methodParam", "control", "forBlock", "ifBlock")

        val names = mutableSetOf<String>()
        val recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitIdentifier(identifier: J.Identifier, p: ExecutionContext): J.Identifier {
                    return if (identifier.simpleName == scope) {
                        val blockCursor: Cursor = this.cursor.dropParentUntil { it is J.Block }
                        names.addAll(VariableNameUtils.findNamesInScope(blockCursor))
                        identifier.withSimpleName("changed")
                    } else {
                        identifier
                    }
                }
            }
        }

        recipe.run(sources)
        val result = names.toSet()

        assertThat(result).containsAll(expected)
        assertThat(expected).containsAll(result)
    }

    @Test
    fun detectMethodParam() {
        // language=java
        val source = arrayOf("""
            class Test {
                void method(boolean methodParam) {
                    boolean methodBlockA;
                    if (methodParam) {
                        boolean ifBlock;
                    }
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, "ifBlock", "ifBlock, methodBlockA, methodParam")
    }

    @Test
    fun innerClass() {
        // language=java
        val source = arrayOf("""
            class Test {
                boolean classBlockA;
                void method() {
                    boolean methodBlock;
                }
                boolean classBlockB;
                class Inner {
                    boolean innerClassBlock;
                }
            }
        """.trimIndent())

        baseTest(source, "innerClassBlock", "classBlockA, classBlockB, innerClassBlock")
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "control: control, methodBlockA",
            "forBlock: forBlock, control, methodBlockA"
        ], delimiter = ':'
    )
    fun forLoop(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            class Test {
                void method() {
                    boolean methodBlockA;
                    for (int control = 0; control < 10; control++) {
                        boolean forBlock;
                    }
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "ifScope: ifScope, methodParam, methodBlockA",
            "elseIfScope: elseIfScope, methodParam, methodBlockA",
            "elseScope: elseScope, methodParam, methodBlockA"
        ], delimiter = ':'
    )
    fun ifElse(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            class Test {
                void method(short methodParam) {
                    boolean methodBlockA;
                    if (methodParam == 0) {
                        boolean ifScope;
                    } else if (methodParam == 1) {
                        boolean elseIfScope;
                    } else {
                        boolean elseScope;
                    }
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @Suppress("UnnecessaryLocalVariable", "Convert2Lambda")
    @ParameterizedTest
    @CsvSource(
        value = [
            "supplier: supplier, methodBlockA",
            "anonMethodBlock: anonMethodBlock, methodBlockA, supplier"
        ], delimiter = ':'
    )
    fun lambda(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            import java.util.function.Supplier;
            
            class Test {
                void method() {
                    boolean methodBlockA;
                    Supplier<Integer> supplier = new Supplier<Integer>() {
                        @Override
                        public Integer get() {
                            int anonMethodBlock;
                            return anonMethodBlock;
                        }
                    };
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "classBlock: classBlock," +
            "superPublic, superProtected, superPackagePrivate," +
            "superSuperPublic, superSuperProtected, superSuperPackagePrivate"
        ], delimiter = ':'
    )
    fun superClass(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            package foo.bar;
            
            class SuperSuper {
                public int superSuperPublic;
                protected int superSuperProtected;
                private int superSuperPrivate;
                int superSuperPackagePrivate;
            }
        """.trimIndent(), """
            package foo.bar;
            
            class Super extends SuperSuper {
                public int superPublic;
                protected int superProtected;
                private int superPrivate;
                int superPackagePrivate;
            }
        """.trimIndent(), """
            package foo.bar;
            
            class Test extends Super {
                boolean classBlock;
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @Suppress("DuplicateBranchesInSwitch")
    @ParameterizedTest
    @CsvSource(
        value = [
            "caseA: caseA, methodParam, methodBlockA",
            "caseB: caseB, methodParam, methodBlockA",
            "defaultBlock: defaultBlock, methodParam, methodBlockA"
        ], delimiter = ':'
    )
    fun switch(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            class Test {
                void method(short methodParam) {
                    boolean methodBlockA;
                    switch (methodParam) {
                        case 0:
                            boolean caseA;
                            break;
                        case 1:
                            boolean caseB;
                            break;
                        default:
                            boolean defaultBlock;
                            break;
                    }
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @Suppress("CatchMayIgnoreException")
    @ParameterizedTest
    @CsvSource(
        value = [
            "resourceA: resourceA, methodBlockA",
            "tryBlock: tryBlock, methodBlockA, resourceA, resourceB",
            "catchControl: catchControl, methodBlockA, resourceA, resourceB",
            "catchBlock: catchBlock, methodBlockA, resourceA, resourceB, catchControl",
            "finallyBlock: finallyBlock, methodBlockA, resourceA, resourceB"
        ], delimiter = ':'
    )
    fun tryCatchFinally(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            import java.io.*;
            
            class Test {
                void method() {
                    File methodBlockA = new File("file.txt");
                    try (FileInputStream resourceA = new FileInputStream(methodBlockA); FileInputStream resourceB = new FileInputStream(methodBlockA)) {
                        boolean tryBlock;
                    } catch (RuntimeException | IOException catchControl) {
                        boolean catchBlock;
                    } finally {
                        boolean finallyBlock;
                    }
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "whileBlock: whileBlock, methodParam, methodBlockA",
            "doWhileBlock: doWhileBlock, methodParam, methodBlockA"
        ], delimiter = ':'
    )
    fun whileLoops(scope: String, result: String) {
        // language=java
        val source = arrayOf("""
            import java.io.*;
            
            class Test {
                void method(short methodParam) {
                    boolean methodBlockA;
                    while (methodParam < 10) {
                        boolean whileBlock;
                        methodParam++;
                    }
                    do {
                        boolean doWhileBlock;
                        methodParam--;
                    } while (methodParam > 0);
                    boolean methodBlockB;
                }
            }
        """.trimIndent())

        baseTest(source, scope, result)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1937")
    @Test
    fun generateUniqueNameWithIncrementedNumber() = rewriteRun(
        { spec ->
            spec.recipe(toRecipe {
                object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitIdentifier(identifier: J.Identifier, p: ExecutionContext): J.Identifier {
                        return if (identifier.simpleName == "ex") {
                            identifier.withSimpleName(VariableNameUtils.generateVariableName("ignored", this.cursor, VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER))
                        } else {
                            identifier
                        }
                    }
                }
            })
        },
        java("""
            @SuppressWarnings("all")
            class Test {
                int ignored = 0;
                void method(int ignored1) {
                    int ignored2 = 0;
                    for (int ignored3 = 0; ignored3 < 10; ignored3++) { // scope does not apply.
                        int ignored4 = 0; // scope does not apply.
                    }
                    if (ignored1 > 0) {
                        int ignored5 = 0; // scope does not apply.
                    }
                    try {
                        int ignored6 = 0; // scope does not apply.
                    } catch (Exception ex) {
                    }
                }
            }
        """, """
            @SuppressWarnings("all")
            class Test {
                int ignored = 0;
                void method(int ignored1) {
                    int ignored2 = 0;
                    for (int ignored3 = 0; ignored3 < 10; ignored3++) { // scope does not apply.
                        int ignored4 = 0; // scope does not apply.
                    }
                    if (ignored1 > 0) {
                        int ignored5 = 0; // scope does not apply.
                    }
                    try {
                        int ignored6 = 0; // scope does not apply.
                    } catch (Exception ignored3) {
                    }
                }
            }
        """)
    )

    fun baseTest(source: Array<String>, scope: String, expected: String) {
        val sources = parser.parse(
            InMemoryExecutionContext(),
            *source
        )

        val expectedResult = expected.split(",").map { it.trim() }.toSet()
        val names = mutableSetOf<String>()
        val recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitIdentifier(identifier: J.Identifier, p: ExecutionContext): J.Identifier {
                    return if (identifier.simpleName == scope) {
                        names.addAll(VariableNameUtils.findNamesInScope(this.cursor))
                        identifier.withSimpleName("changed")
                    } else {
                        identifier
                    }
                }
            }
        }

        recipe.run(sources)
        val result = names.toSet()
        assertThat(result).containsAll(expectedResult)
        assertThat(expectedResult).containsAll(result)
    }
}
