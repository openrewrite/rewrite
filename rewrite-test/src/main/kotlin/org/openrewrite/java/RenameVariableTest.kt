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
import org.openrewrite.*
import org.openrewrite.java.tree.J

interface RenameVariableTest : RecipeTest {
    @Test
    fun renameVariable(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            override fun visitMultiVariable(multiVariable: J.VariableDecls, p: ExecutionContext): J {
                val varCursor = Cursor(cursor, multiVariable.vars[0].elem)
                if (cursor.dropParentUntil { it is J }.getValue<J>() is J.MethodDecl) {
                    doAfterVisit(RenameVariable(varCursor, "n2"))
                } else if (cursor
                        .dropParentUntil { it is J }
                        .dropParentUntil { it is J }
                        .getValue<J>() !is J.ClassDecl) {
                    doAfterVisit(RenameVariable(varCursor, "n1"))
                }
                return super.visitMultiVariable(multiVariable, p)
            }
        }.toRecipe(cursored = true),
        before = """
            public class B {
               int n;
            
               {
                   int n;
                   n = 1;
                   n /= 2;
                   if(n + 1 == 2) {}
                   n++;
               }
               
               public int foo(int n) {
                   return n + this.n;
               }
            }
        """,
        after = """
            public class B {
               int n;
            
               {
                   int n1;
                   n1 = 1;
                   n1 /= 2;
                   if(n1 + 1 == 2) {}
                   n1++;
               }
               
               public int foo(int n2) {
                   return n2 + this.n;
               }
            }
        """
    )
}
