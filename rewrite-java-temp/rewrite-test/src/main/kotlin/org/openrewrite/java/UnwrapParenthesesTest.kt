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
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.J

interface UnwrapParenthesesTest : RefactorVisitorTest {
    @Test
    fun unwrapAssignment(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a: J.CompilationUnit ->
                val parens = ((a.classes[0].body.statements[1] as J.Block<*>).statements[0] as J.Assign)
                        .assignment as J.Parentheses<*>
                UnwrapParentheses.Scoped(parens)
            },
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
    fun unwrapIfCondition(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                val parens = ((a.classes[0].body.statements[0] as J.Block<*>)
                        .statements[0] as J.If)
                        .ifCondition.tree as J.Parentheses<*>
                UnwrapParentheses.Scoped(parens)
            },
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
