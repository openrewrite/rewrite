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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface RenameExceptionInEmptyCatchTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RenameExceptionInEmptyCatch()

    @Test
    fun notEmpty() = assertUnchanged(
        before = """
            @SuppressWarnings("all")
            class Test {
                void method() {
                    try {
                    } catch (Exception ex) {
                        // comment
                    }
                }
            }
        """
    )

    @Test
    fun nameScopeTest() = assertChanged(
        before = """
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
        """,
        after = """
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
        """
    )

    @Test
    fun emptyCatchBlock() = assertChanged(
        before = """
            @SuppressWarnings("all")
            class Test {
                void method() {
                    try {
                    } catch (Exception ex) {
                    }
                }
            }
        """,
        after = """
            @SuppressWarnings("all")
            class Test {
                void method() {
                    try {
                    } catch (Exception ignored) {
                    }
                }
            }
        """
    )

    @SuppressWarnings("all")
    @Test
    fun multipleCatches() = assertChanged(
        before = """
            @SuppressWarnings("all")
            class Test {
                void method() {
                    try {
                    } catch (IllegalArgumentException ex) {
                    } catch (IllegalAccessException ex) {
                    }
                }
            }
        """,
        after = """
            @SuppressWarnings("all")
            class Test {
                void method() {
                    try {
                    } catch (IllegalArgumentException ignored) {
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        """
    )
}
