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
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.J

@ExtendWith(JavaParserResolver::class)
interface ChangeClassVisibilityTest: RefactorVisitorTest {

    @Test
    fun makePublicClassProtected(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeClassVisibility.Scoped(cu.classes.first(), "protected")
            },
            before = """
                public class A { }
            """,
            after = """
                class A { }
            """
    )

    @Test
    fun makeProtectedClassPublic(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeClassVisibility.Scoped(cu.classes.first(), "public")
            },
            before = """
                class A {
                }
            """,
            after = """
                public class A {
                }
            """
    )

    @Test
    fun makeInnerPrivateClassPublic(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeClassVisibility.Scoped(cu.classes.first().body.statements.first() as J.ClassDecl?, "public")
            },
            before = """
                class A {
                    private class B { }
                }
            """,
            after = """
                class A {
                    public class B { }
                }
            """
    )

    @Test
    fun makeInnerPrivateClassProtected(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeClassVisibility.Scoped(cu.classes.first().body.statements.first() as J.ClassDecl?, "protected")
            },
            before = """
                class A {
                    private class B { }
                }
            """,
            after = """
                class A {
                    class B { }
                }
            """
    )

    @Test
    fun makeInnerProtectedClassPrivate(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu: J.CompilationUnit ->
                ChangeClassVisibility.Scoped(cu.classes.first().body.statements.first() as J.ClassDecl?, "private")
            },
            before = """
                class A {
                    class B { }
                }
            """,
            after = """
                class A {
                    private class B { }
                }
            """
    )
}
