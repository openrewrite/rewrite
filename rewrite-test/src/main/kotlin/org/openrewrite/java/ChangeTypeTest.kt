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

interface ChangeTypeTest {
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
    fun dontAddImportWhenNoChangesWereMade(jp: JavaParser) {
        val b = jp.parse("public class B {}")
        val fixed = b.refactor().visit(changeType).fix().fixed
        assertRefactored(fixed, "public class B {}")
    }

    @Test
    fun simpleName(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            
            public class B extends A1 {}
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            
            public class B extends A2 {}
        """)
    }

    @Test
    fun fullyQualifiedName(jp: JavaParser) {
        val b = jp.parse("public class B extends a.A1 {}", a1, a2)
        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            public class B extends a.A2 {}
        """)
    }

    @Test
    fun annotation(jp: JavaParser) {
        val a1 = "package a;\npublic @interface A1 {}"
        val a2 = "package a;\npublic @interface A2 {}"
        val b = jp.parse("@a.A1 public class B {}", a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, "@a.A2 public class B {}")
    }

    @Test
    fun array(jp: JavaParser) { // array types and new arrays
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1[] a = new A1[0];
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               A2[] a = new A2[0];
            }
        """)
    }

    @Test
    fun classDecl(jp: JavaParser) {
        val i1 = "public interface I1 {}"
        val i2 = "public interface I2 {}"

        val b = jp.parse("""
            import a.A1;
            public class B extends A1 implements I1 {}
        """.trimIndent(), a1, a2, i1, i2)

        val fixed = b.refactor()
                .visit(changeType)
                .visit(ChangeType().apply { setType("I1"); setTargetType("I2") })
                .fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B extends A2 implements I2 {}
        """)
    }

    @Test
    fun method(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               public A1 foo() throws A1 { return null; }
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               public A2 foo() throws A2 { return null; }
            }
        """)
    }

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in);
               public void test() {
                   A1.stat();
                   this.<A1>generic(null, null);
               }
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               public <T extends A2> T generic(T n, List<? super A2> in);
               public void test() {
                   A2.stat();
                   this.<A2>generic(null, null);
               }
            }
        """)
    }

    @Test
    fun multiCatch(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               public void test() {
                   try {}
                   catch(A1 | RuntimeException e) {}
               }
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               public void test() {
                   try {}
                   catch(A2 | RuntimeException e) {}
               }
            }
        """)
    }

    @Test
    fun multiVariable(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 f1, f2;
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               A2 f1, f2;
            }
        """)
    }

    @Test
    fun newClass(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 a = new A1();
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               A2 a = new A2();
            }
        """)
    }

    @Test
    fun parameterizedType(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               Map<A1, A1> m;
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               Map<A2, A2> m;
            }
        """)
    }

    @Test
    fun typeCast(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 a = (A1) null;
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               A2 a = (A2) null;
            }
        """)
    }

    @Test
    fun classReference(jp: JavaParser) {
        val a = jp.parse("""
            import a.A1;
            public class A {
                Class<?> clazz = A1.class;
            }
        """.trimIndent(), a1, a2)

        val fixed = a.refactor().visit(changeType).fix().fixed
        assertRefactored(fixed, """
            import a.A2;
            public class A {
                Class<?> clazz = A2.class;
            }
        """)
    }

    @Test
    fun methodSelect(jp: JavaParser) {
        val b = jp.parse("""
            import a.A1;
            public class B {
               A1 a = null;
               public void test() { a.foo(); }
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed

        val inv = fixed.classes[0].methods[0].body!!.statements[0]
        println(inv)

        assertRefactored(fixed, """
            import a.A2;
            public class B {
               A2 a = null;
               public void test() { a.foo(); }
            }
        """)
    }

    @Test
    fun staticImport(jp: JavaParser) {
        val b = jp.parse("""
            import static a.A1.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """.trimIndent(), a1, a2)

        val fixed = b.refactor().visit(changeType).fix().fixed
        assertRefactored(fixed, """
            import static a.A2.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """.trimIndent())
    }

}
