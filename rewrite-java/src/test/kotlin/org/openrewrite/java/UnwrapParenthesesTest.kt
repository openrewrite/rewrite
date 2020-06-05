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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.UnwrapParentheses
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

class UnwrapParenthesesTest : JavaParser() {
    @Test
    fun unwrapAssignment() {
        val a = parse("""
            public class A {
                boolean a;
                {
                    a = (true);
                }
            }
        """.trimIndent())

        val assignment = ((a.classes[0].body.statements[1] as J.Block<*>).statements[0] as J.Assign).assignment as J.Parentheses<*>
        val fixed = a.refactor().visit(UnwrapParentheses(assignment)).fix().fixed

        assertRefactored(fixed, """
            public class A {
                boolean a;
                {
                    a = true;
                }
            }
        """)
    }

    @Test
    fun unwrapIfCondition() {
        val a = parse("""
            public class A {
                {
                    if((true)) {}
                }
            }
        """.trimIndent())

        val cond = ((a.classes[0].body.statements[0] as J.Block<*>).statements[0] as J.If).ifCondition.tree as J.Parentheses<*>
        val fixed = a.refactor().visit(UnwrapParentheses(cond)).fix().fixed

        assertRefactored(fixed, """
            public class A {
                {
                    if(true) {}
                }
            }
        """)
    }
}
