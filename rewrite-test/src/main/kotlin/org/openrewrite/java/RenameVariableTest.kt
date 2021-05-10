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
import org.openrewrite.java.tree.J
import java.util.stream.Collectors.toList

interface RenameVariableTest : JavaRecipeTest {
    @Test
    fun doNotRenameConstant(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                val variableDecls = classDecl.body.statements.stream().filter {s -> s is J.VariableDeclarations}
                    .map {s -> s as J.VariableDeclarations}
                    .filter {v -> v.hasModifier(J.Modifier.Type.Static) && v.hasModifier(J.Modifier.Type.Final)}
                    .collect(toList())

                val namedVariables: MutableList<J.VariableDeclarations.NamedVariable> = mutableListOf()
                variableDecls.forEach { namedVariables.addAll(it.variables) }

                for (namedVariable in namedVariables) {
                    if (namedVariable.simpleName.equals("n")) {
                        doAfterVisit(RenameVariable(namedVariable, "N"))
                    }
                }

                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            public class B {
                static final int n;
            
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
        """
    )

    @Test
    fun renameVariable(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            override fun visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): J {
                if (cursor.dropParentUntil { it is J }.getValue<J>() is J.MethodDeclaration) {
                    doAfterVisit(RenameVariable(multiVariable.variables[0], "n2"))
                } else if (cursor
                        .dropParentUntil { it is J }
                        .dropParentUntil { it is J }
                        .getValue<J>() !is J.ClassDeclaration
                ) {
                    doAfterVisit(RenameVariable(multiVariable.variables[0], "n1"))
                }
                return super.visitVariableDeclarations(multiVariable, p)
            }
        }.toRecipe(),
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
