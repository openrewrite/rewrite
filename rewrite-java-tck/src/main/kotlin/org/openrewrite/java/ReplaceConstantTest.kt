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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe

interface ReplaceConstantTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = ReplaceConstant("com.google.common.base.Charsets", "UTF_8", "\"UTF_8\"")

    @Test
    fun replaceConstant(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath("guava").build(),
        before = """ 
            import com.google.common.base.Charsets;
            class Test {
                Object o = Charsets.UTF_8;
            }
        """,
        after = """
            class Test {
                Object o = "UTF_8";
            }
        """
    )

    @Test
    fun replaceStaticallyImportedConstant(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath("guava").build(),
        before = """ 
            import static com.google.common.base.Charsets.UTF_8;
            class Test {
                Object o = UTF_8;
            }
        """,
        after = """
            class Test {
                Object o = "UTF_8";
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1752")
    @Test
    fun doesNotChangeOriginalVariableDeclaration() = assertChanged(
        dependsOn = arrayOf("""
            package com.constant;
            public class B {
                public static final String VAR = "default";
                void method() {
                    String VAR = "";
                }
            }
        """),
        recipe = ReplaceConstant("com.constant.B", "VAR", "\"newValue\""),
        before = """
            package com.abc;
            import com.constant.B;
            class A {
                String v = B.VAR;
            }
        """,
        after = """
            package com.abc;
            class A {
                String v = "newValue";
            }
        """
    )
}
