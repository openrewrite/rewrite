/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

open class RenameVariableTest : JavaParser() {
    @Test
    fun renameVariable() {
        val a = parse("""
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
        """.trimIndent())

        val blockN = (a.classes[0].body.statements[1] as J.Block<*>).statements[0] as J.VariableDecls
        val paramN = (a.classes[0].methods[0]).params.params[0] as J.VariableDecls

        val fixed = a.refactor()
                .visit(RenameVariable(blockN.vars[0], "n1"))
                .visit(RenameVariable(paramN.vars[0], "n2"))
                .fix().fixed

        assertRefactored(fixed, """
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
        """)
    }
}