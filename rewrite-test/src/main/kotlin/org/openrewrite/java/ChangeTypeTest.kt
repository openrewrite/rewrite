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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest

interface ChangeTypeTest : RefactorVisitorTest {
    companion object {
        private val changeType = ChangeType().apply { setType("a.A1"); setTargetType("a.A2") }

        private val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()

        private val a2 = """
            package a;
            public class A2 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
    }

    @Test
    fun dontAddImportWhenNoChangesWereMade(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(changeType),
            before = "public class B {}"
    )

    @Test
    fun simpleName(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                
                public class B extends A1 {}
            """,
            after = """
                import a.A2;
                
                public class B extends A2 {}
            """
    )

    @Test
    fun fullyQualifiedName(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = "public class B extends a.A1 {}",
            after = "public class B extends a.A2 {}"
    )

    @Test
    fun annotation(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                "package a;\npublic @interface A1 {}",
                "package a;\npublic @interface A2 {}"
            ),
            visitors = listOf(changeType),
            before = "@a.A1 public class B {}",
            after = "@a.A2 public class B {}"
    )

    // array types and new arrays
    @Test
    fun array(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   A1[] a = new A1[0];
                }
            """,
            after = """
                import a.A2;
                public class B {
                   A2[] a = new A2[0];
                }
            """
    )

    @Test
    fun classDecl(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2,
                "public interface I1 {}",
                "public interface I2 {}"
            ),
            visitors = listOf(
                changeType,
                ChangeType().apply { setType("I1"); setTargetType("I2") }
            ),
            before = """
                import a.A1;
                public class B extends A1 implements I1 {}
            """,
            after = """
                import a.A2;
                public class B extends A2 implements I2 {}
            """
    )

    @Test
    fun method(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   public A1 foo() throws A1 { return null; }
                }
            """,
            after = """
                import a.A2;
                public class B {
                   public A2 foo() throws A2 { return null; }
                }
            """
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   public <T extends A1> T generic(T n, List<? super A1> in);
                   public void test() {
                       A1.stat();
                       this.<A1>generic(null, null);
                   }
                }
            """,
            after = """
                import a.A2;
                public class B {
                   public <T extends A2> T generic(T n, List<? super A2> in);
                   public void test() {
                       A2.stat();
                       this.<A2>generic(null, null);
                   }
                }
            """
    )

    @Test
    fun multiCatch(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   public void test() {
                       try {}
                       catch(A1 | RuntimeException e) {}
                   }
                }
            """,
            after = """
                import a.A2;
                public class B {
                   public void test() {
                       try {}
                       catch(A2 | RuntimeException e) {}
                   }
                }
            """
    )

    @Test
    fun multiVariable(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   A1 f1, f2;
                }
            """,
            after = """
                import a.A2;
                public class B {
                   A2 f1, f2;
                }
            """
    )

    @Test
    fun newClass(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   A1 a = new A1();
                }
            """,
            after = """
                import a.A2;
                public class B {
                   A2 a = new A2();
                }
            """
    )

    @Test
    fun parameterizedType(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   Map<A1, A1> m;
                }
            """,
            after = """
                import a.A2;
                public class B {
                   Map<A2, A2> m;
                }
            """
    )

    @Test
    fun typeCast(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   A1 a = (A1) null;
                }
            """,
            after = """
                import a.A2;
                public class B {
                   A2 a = (A2) null;
                }
            """
    )

    @Test
    fun classReference(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class A {
                    Class<?> clazz = A1.class;
                }
            """,
            after = """
                import a.A2;
                public class A {
                    Class<?> clazz = A2.class;
                }
            """
    )

    @Test
    fun methodSelect(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import a.A1;
                public class B {
                   A1 a = null;
                   public void test() { a.foo(); }
                }
            """,
            after = """
                import a.A2;
                public class B {
                   A2 a = null;
                   public void test() { a.foo(); }
                }
            """
    )

    @Test
    fun staticImport(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(a1, a2),
            visitors = listOf(changeType),
            before = """
                import static a.A1.stat;
                public class B {
                    public void test() {
                        stat();
                    }
                }
            """,
            after = """
                import static a.A2.stat;
                public class B {
                    public void test() {
                        stat();
                    }
                }
            """
    )

    @Disabled("https://github.com/openrewrite/rewrite/issues/62")
    @Test
    fun primitiveToClass(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(ChangeType().apply {
                setType("int")
                setTargetType("java.lang.Integer")
            }),
            before = """
                class A {
                    int foo = 5;
                    int getFoo() {
                        return foo;
                    }
                }
            """,
            after = """
                class A {
                    Integer foo = 5;
                    Integer getFoo() {
                        return foo;
                    }
                }
            """
    )

    @Disabled("https://github.com/openrewrite/rewrite/issues/62")
    @Test
    fun classToPrimitive(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(ChangeType().apply {
                setType("java.lang.Integer")
                setTargetType("int")
            }),
            before = """
                class A {
                    Integer foo = 5;
                    Integer getFoo() {
                        return foo;
                    }
                }
            """,
            after = """
                class A {
                    int foo = 5;
                    int getFoo() {
                        return foo;
                    }
                }
            """
    )
}
