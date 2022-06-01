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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("UnusedAssignment", "EmptyTryBlock", "TryWithIdenticalCatches", "CatchMayIgnoreException")
interface CombineSemanticallyEqualCatchBlocksTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(CombineSemanticallyEqualCatchBlocks())
    }

    @Test
    fun doNotCombineDifferentCatchBlocks(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                        String s = "foo";
                    } catch (B ex) {
                        String s = "bar";
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun childClassIsCaughtBeforeParentClass(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends BaseException {}"),
        java("class BaseException extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                    } catch (B ex) { // Is subtype of BaseException with a unique block.
                        String diff;
                    } catch (BaseException ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun blocksContainDifferentComments(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                        // Comment 1
                    } catch (B ex) {
                        // Comment 2
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun blocksContainSameComments(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                        // Same
                    } catch (B ex) {
                        // Same
                    }
                }
            }
        """.trimIndent(),
        """
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                        // Same
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun combineSameSemanticallyEquivalentMethodTypes(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends BaseException {}"),
        java("class B extends BaseException {}"),
        java("class BaseException extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                        base(ex);
                    } catch (B ex) {
                        base(ex);
                    }
                }
                void base(BaseException ex) {}
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                        base(ex);
                    }
                }
                void base(BaseException ex) {}
            }
        """.trimIndent())
    )

    @Test
    fun combineCatchesIntoNewMultiCatch(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                    } catch (B ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun fromMultiCatchCombineWithCatch(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("class C extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                    } catch (B | C ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | B | C ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun fromCatchCombineWithMultiCatch(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("class C extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                    } catch (C ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | B | C ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun fromMultiCatchCombineWithMultiCatch(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends RuntimeException {}"),
        java("class C extends RuntimeException {}"),
        java("class D extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                    } catch (C | D ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | B | C | D ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun preserveOrderOfCatchesWhenPossible(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends RuntimeException {}"),
        java("class B extends BaseException {}"),
        java("class C extends BaseException {}"),
        java("class BaseException extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                    } catch (B ex) {
                        String diff;
                    } catch (C ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (A | C ex) {
                    } catch (B ex) {
                        String diff;
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun removeRedundantChildClasses(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends BaseException {}"),
        java("class B extends RuntimeException {}"),
        java("class BaseException extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A ex) {
                    } catch (B ex) {
                    } catch (BaseException ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (B | BaseException ex) {
                    }
                }
            }
        """.trimIndent())
    )

    @Test
    fun removeRedundantChildClassesWithExistingMultiCatches(jp: JavaParser) = rewriteRun(
        {spec -> spec.parser(jp)},
        java("class A extends BaseException {}"),
        java("class B extends RuntimeException {}"),
        java("class BaseException extends RuntimeException {}"),
        java("class Other extends RuntimeException {}"),
        java("""
            class Test {
                void method() {
                    try {
                    } catch (A | B ex) {
                    } catch (BaseException | Other ex) {
                    }
                }
            }
        """.trimIndent(),
            """
            class Test {
                void method() {
                    try {
                    } catch (B | BaseException | Other ex) {
                    }
                }
            }
        """.trimIndent())
    )
}
