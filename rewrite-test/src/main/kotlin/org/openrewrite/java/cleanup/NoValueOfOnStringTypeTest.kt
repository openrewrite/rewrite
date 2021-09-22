/*
 * Copyright 2021 the original author or authors.
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

interface NoValueOfOnStringTypeTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = NoValueOfOnStringType()

    @Test
    fun doNotChangeOnObject() = assertUnchanged(
        before = """
            class Test {
                String method() {
                    Object obj;
                    return String.valueOf(obj);
                }
            }
        """
    )

    @Test
    fun valueOfOnString() = assertChanged(
        before = """
            class Test {
                @SuppressWarnings("all")
                String method() {
                    return String.valueOf("hello");
                }
            } 
        """,
        after = """
            class Test {
                @SuppressWarnings("all")
                String method() {
                    return "hello";
                }
            } 
        """
    )

    @Test
    fun valueOfOnStringVariable() = assertChanged(
        before = """
            class Test {
                String method(String val) {
                    return String.valueOf(val);
                }
            }
        """,
        after = """
            class Test {
                String method(String val) {
                    return val;
                }
            }
        """
    )

    @Test
    fun valueOfOnMethodInvocation() = assertChanged(
        before = """
            class Test {
                void method1() {
                    String a = String.valueOf(method2());
                }
                String method2() {
                    return "";
                }
            }
        """,
        after = """
            class Test {
                void method1() {
                    String a = method2();
                }
                String method2() {
                    return "";
                }
            }
        """
    )
}
