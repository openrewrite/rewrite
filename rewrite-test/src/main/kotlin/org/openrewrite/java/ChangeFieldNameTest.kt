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
import org.openrewrite.Issue
import org.openrewrite.java.tree.J

interface ChangeFieldNameTest : JavaRecipeTest {
    fun changeFieldName(enclosingClassFqn: String, from: String, to: String) = toRecipe {
        object : JavaIsoVisitor<ExecutionContext>() {
            override fun visitCompilationUnit(cu: J.CompilationUnit, p: ExecutionContext): J.CompilationUnit {
                doAfterVisit(ChangeFieldName(enclosingClassFqn, from, to))
                return super.visitCompilationUnit(cu, p)
            }
        }
    }

    @Suppress("rawtypes")
    @Test
    fun changeFieldName(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("Test", "collection", "list"),
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

    @Suppress("StatementWithEmptyBody", "ConstantConditions")
    @Test
    fun changeFieldNameReferences(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldName("Test", "n", "n1"),
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
        recipe = changeFieldName("Test", "n", "n1"),
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
    @Issue("https://github.com/openrewrite/rewrite/issues/632")
    fun changeFieldNameReferencesInOtherClassUsingStaticImport(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.example;

                public class Test {
                    public static final int IMPORT_ME_STATICALLY = 0;
                }
            """
        ),
        recipe = changeFieldName("com.example.Test", "IMPORT_ME_STATICALLY", "IMPORT_ME_STATICALLY_1"),
        before = """
            package org.openrewrite.test;

            import static com.example.Test.IMPORT_ME_STATICALLY;

            public class Caller {
                int e = IMPORT_ME_STATICALLY;
            }
        """,
        after = """
            package org.openrewrite.test;

            import static com.example.Test.IMPORT_ME_STATICALLY_1;

            public class Caller {
                int e = IMPORT_ME_STATICALLY_1;
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun dontChangeNestedFieldsWithSameName(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("class A { Object collection; }"),
        recipe = changeFieldName("Test", "collection", "list"),
        before = """
            import java.util.List;
            class Test {
                List collection = null;
                class Nested {
                    Object collection = Test.this.collection;
                    Object collection2 = A.this.collection;
                }
            }
        """,
        after = """
            import java.util.List;
            class Test {
                List list = null;
                class Nested {
                    Object collection = Test.this.list;
                    Object collection2 = A.this.collection;
                }
            }
        """
    )

}
