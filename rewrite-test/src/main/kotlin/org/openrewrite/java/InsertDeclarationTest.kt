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
import org.openrewrite.java.tree.TreeBuilder

interface InsertDeclarationTest : RefactorVisitorTest {
    @Test
    fun insertFirst(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu ->
                InsertDeclaration.Scoped(
                        cu.classes[0],
                        TreeBuilder(cu).buildDeclaration(
                                cu.classes[0],
                                "void setSomething() {}"
                        )
                )
            },
            before = """
                class A {
                }
            """,
            after = """
                class A {
                
                    void setSomething() {}
                }
            """
    )

    @Test
    fun insertLast(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu ->
                InsertDeclaration.Scoped(
                        cu.classes[0],
                        TreeBuilder(cu).buildDeclaration(
                                cu.classes[0],
                                "void setSomething() {}"
                        )
                )
            },
            before = """
                class A {
                    String getSomething() {
                        return "something";
                    }
                }
            """,
            after = """
                class A {
                    String getSomething() {
                        return "something";
                    }
                
                    void setSomething() {}
                }
            """
    )

    @Test
    fun insertMiddle(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu ->
                InsertDeclaration.Scoped(
                        cu.classes[0],
                        TreeBuilder(cu).buildDeclaration(
                                cu.classes[0],
                                "void setSomething() {}"
                        )
                )
            },
            before = """
                class A {
                    private final String s;
                
                    public A() {
                    }
                
                    String getSomething() {
                        return "something";
                    }
                
                    String toString() {
                        return "string";
                    }
                }
            """,
            after = """
                class A {
                    private final String s;
                
                    public A() {
                    }
                
                    String getSomething() {
                        return "something";
                    }
                
                    void setSomething() {}
                
                    String toString() {
                        return "string";
                    }
                }
            """
    )
}
