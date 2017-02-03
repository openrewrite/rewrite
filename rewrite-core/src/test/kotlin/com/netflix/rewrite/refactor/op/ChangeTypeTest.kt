/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

abstract class ChangeTypeTest(p: Parser): Parser by p {

    val a1 = """
        package a;
        public class A1 extends Exception {
            public static void stat() {}
            public void foo() {}
        }
    """

    val a2 = """
        package a;
        public class A2 extends Exception {
            public static void stat() {}
            public void foo() {}
        }
    """

    @Test
    fun dontAddImportWhenNoChangesWereMade() {
        val b = parse("public class B {}")
        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()
        assertRefactored(fixed, "public class B {}")
    }

    @Test
    fun simpleName() {
        val b = parse("""
            |import a.A1;
            |
            |public class B extends A1 {}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B extends A2 {}
        """)
    }

    @Test
    fun fullyQualifiedName() {
        val b = parse("public class B extends a.A1 {}", a1, a2)
        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B extends A2 {}
        """)
    }

    @Test
    fun annotation() {
        val a1 = "public @interface A1 {}"
        val a2 = "public @interface A2 {}"
        val b = parse("@A1 public class B {}", a1, a2)
        val fixed = b.refactor().changeType("A1", "A2").fix()
        assertRefactored(fixed, "@A2 public class B {}")
    }

    @Test
    fun array() { // array types and new arrays
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1[] a = new A1[0];
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   A2[] a = new A2[0];
            |}
        """)
    }

    @Test
    fun classDecl() {
        val i1 = "public interface I1 {}"
        val i2 = "public interface I2 {}"

        val b = parse("""
            |import a.A1;
            |public class B extends A1 implements I1 {}
        """, a1, a2, i1, i2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").changeType("I1", "I2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B extends A2 implements I2 {}
        """)
    }

    @Test
    fun method() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   public A1 foo() throws A1 { return null; }
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   public A2 foo() throws A2 { return null; }
            |}
        """)
    }

    @Test
    fun methodInvocationTypeParametersAndWildcard() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   public <T extends A1> T generic(T n, List<? super A1> in);
            |   public void test() {
            |       A1.stat();
            |       this.<A1>generic(null, null);
            |   }
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   public <T extends A2> T generic(T n, List<? super A2> in);
            |   public void test() {
            |       A2.stat();
            |       this.<A2>generic(null, null);
            |   }
            |}
        """)
    }

    @Test
    fun multiCatch() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   public void test() {
            |       try {}
            |       catch(A1 | RuntimeException e) {}
            |   }
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   public void test() {
            |       try {}
            |       catch(A2 | RuntimeException e) {}
            |   }
            |}
        """)
    }

    @Test
    fun multiVariable() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 f1, f2;
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   A2 f1, f2;
            |}
        """)
    }

    @Test
    fun newClass() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 a = new A1();
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   A2 a = new A2();
            |}
        """)
    }

    @Test
    fun paramaterizedType() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   Map<A1, A1> m;
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   Map<A2, A2> m;
            |}
        """)
    }

    @Test
    fun typeCast() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 a = (A1) null;
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   A2 a = (A2) null;
            |}
        """)
    }

    @Test
    fun classReference() {
        val a = parse("""
            |import a.A1;
            |public class A {
            |    Class<?> clazz = A1.class;
            |}
        """, a1, a2)

        val fixed = a.refactor().changeType("a.A1", "a.A2").fix()
        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class A {
            |    Class<?> clazz = A2.class;
            |}
        """)
    }

    @Test
    fun `even though references to original type remain in the ast, the original type's import is removed`() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 a = null;
            |   public void test() { a.foo(); }
            |}
        """, a1, a2)

        val fixed = b.refactor().changeType("a.A1", "a.A2").fix()

        val inv = fixed.classes[0].methods()[0].body!!.statements[0]
        println(inv)

        assertRefactored(fixed, """
            |import a.A2;
            |
            |public class B {
            |   A2 a = null;
            |   public void test() { a.foo(); }
            |}
        """)
    }
}

class OracleJdkChangeTypeTest: ChangeTypeTest(OracleJdkParser())