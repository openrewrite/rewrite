/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J

interface UnwrapParenthesesTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = object : TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaVisitor<ExecutionContext>() {
                    override fun <T : J?> visitParentheses(parens: J.Parentheses<T>, p: ExecutionContext): J {
                        doAfterVisit(UnwrapParentheses(parens))
                        return super.visitParentheses(parens, p)
                    }
                }
            }
        }

    @Test
    fun unwrapAssignment(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                boolean a;
                {
                    a = (true);
                }
            }
        """,
        after = """
            public class A {
                boolean a;
                {
                    a = true;
                }
            }
        """
    )

    @Test
    fun unwrapIfCondition(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                {
                    if((true)) {}
                }
            }
        """,
        after = """
            public class A {
                {
                    if(true) {}
                }
            }
        """
    )
}
