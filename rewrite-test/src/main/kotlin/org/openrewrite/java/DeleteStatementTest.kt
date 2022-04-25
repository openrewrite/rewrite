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
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J

interface DeleteStatementTest : JavaRecipeTest {

    @Test
    fun deleteField(jp: JavaParser) = assertChanged(
        jp,
        recipe = object: TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object: JavaVisitor<ExecutionContext>() {
                    override fun visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): J {
                        doAfterVisit(DeleteStatement(multiVariable))
                        return super.visitVariableDeclarations(multiVariable, p)
                    }
                }
            }
        },
        before = """
            import java.util.List;
            public class A {
               List collection = null;
            }
        """,
        after = """
            public class A {
            }
        """
    )

    @Test
    fun deleteSecondStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object: TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object: JavaIsoVisitor<ExecutionContext>() {
                    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                        val b = super.visitBlock(block, p)
                        if (b.statements.size != 4) return b
                        b.statements.forEachIndexed { i, s ->
                            if (i == 1) {
                                doAfterVisit(DeleteStatement(s))
                            }
                        }
                        return b
                    }
                }
            }
        },
        before = """
            public class A {
               {
                  String s = "";
                  s.toString();
                  s = "hello";
                  s.toString();
               }
            }
        """,
        after = """
            public class A {
               {
                  String s = "";
                  s = "hello";
                  s.toString();
               }
            }
        """
    )

    @Test
    fun deleteSecondAndFourthStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object: TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object: JavaIsoVisitor<ExecutionContext>() {
                    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                        val b = super.visitBlock(block, p)
                        if (b.statements.size != 4) return b
                        b.statements.forEachIndexed { i, s ->
                            if (i == 1 || i == 3) {
                                doAfterVisit(DeleteStatement(s))
                            }
                        }
                        return b
                    }
                }
            }
        },
        before = """
            public class A {
               {
                  String s = "";
                  s.toString();
                  s = "hello";
                  s.toString();
               }
            }
        """,
        after = """
            public class A {
               {
                  String s = "";
                  s = "hello";
               }
            }
        """
    )
}
