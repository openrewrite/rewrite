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
import org.openrewrite.RecipeTest
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface ChangeFieldNameTest : RecipeTest {
    fun changeFieldName(from: String, to: String) = object : JavaIsoVisitor<ExecutionContext>() {
        override fun visitMultiVariable(v: J.VariableDecls, p: ExecutionContext): J.VariableDecls {
            if (cursor.dropParentUntil { it is J }.dropParentUntil { it is J }.getValue<J>() is J.ClassDecl) {
                doAfterVisit(ChangeFieldName(JavaType.Class.build("Test"), from, to))
            }
            return super.visitMultiVariable(v, p)
        }
    }.toRecipe(cursored = true)

    @Test
    fun changeFieldName(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("collection", "list"),
        before = """
            import java.util.List;
            class Test {
               List collection = null;
            }
        """,
        after = """
            import java.util.List;
            class Test {
               List list = null;
            }
        """
    )

    @Test
    fun changeFieldNameReferences(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("n", "n1"),
        before = """
            class Test {
               int n;
               
               {
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
            class Test {
               int n1;
               
               {
                   n1 = 1;
                   n1 /= 2;
                   if(n1 + 1 == 2) {}
                   n1++;
               }
               
               public int foo(int n) {
                   return n + this.n1;
               }
            }
        """
    )

    @Test
    fun changeFieldNameReferencesInOtherClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("n", "n1"),
        before = """
            class Caller {
                Test t = new Test();
                {
                    t.n = 1;
                }
            }
        """,
        after = """
            class Caller {
                Test t = new Test();
                {
                    t.n1 = 1;
                }
            }
        """,
        dependsOn = arrayOf(
            """
                class Test {
                   int n;
                }
            """
        ),
    )

    @Test
    fun dontChangeNestedFieldsWithSameName(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("collection", "list"),
        before = """
            import java.util.List;
            class Test {
                List collection = null;
                class Nested {
                    Object collection = A.this.collection;
                }
            }
        """,
        after = """
            import java.util.List;
            class Test {
                List list = null;
                class Nested {
                    Object collection = A.this.list;
                }
            }
        """
    )
}
