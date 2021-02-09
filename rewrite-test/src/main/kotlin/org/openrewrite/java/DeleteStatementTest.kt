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
import org.openrewrite.RecipeTest
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J

interface DeleteStatementTest : RecipeTest {

    @Test
    fun deleteField(jp: JavaParser) = assertChanged(
        jp,
        recipe = object: Recipe() {
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
}
